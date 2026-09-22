package com.jam2in.arcus.http.common.adapter;

import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.common.model.BulkOperationResult;
import com.jam2in.arcus.http.common.model.CollectionOperationResult;
import com.jam2in.arcus.http.common.model.PipedOperationResult;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.internal.CompositeException;
import net.spy.memcached.ops.CollectionOperationStatus;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import org.springframework.stereotype.Component;

@Component
public class ClientSupport {

  public static void require(boolean valid) {
    if (!valid) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }
  }

  public static byte[] hex(String value, int maxBytes) {
    if (value == null) {
      return null;
    }
    require(!value.isEmpty() && value.length() % 2 == 0 && value.length() <= maxBytes * 2);

    try {
      return HexFormat.of().parseHex(value);
    } catch (IllegalArgumentException exception) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST, exception);
    }
  }

  public static String hex(byte[] value) {
    return value == null ? null : HexFormat.of().formatHex(value);
  }

  public <T> T call(Supplier<? extends Future<T>> supplier) {
    return result(() -> supplier.get().get());
  }

  public <T> T result(ResultSupplier<T> supplier) {
    try {
      return supplier.get();
    } catch (OperationTimeoutException exception) {
      throw new ArcusHttpException(ErrorCode.ARCUS_TIMEOUT, exception);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ArcusHttpException(ErrorCode.ARCUS_UNAVAILABLE, exception);
    } catch (ExecutionException exception) {
      // Multiple bulk failures expose an overview as the cause, not the individual failures.
      if (exception instanceof CompositeException composite
          && composite.getExceptions().stream().allMatch(CancellationException.class::isInstance)) {
        throw new ArcusHttpException(ErrorCode.ARCUS_UNAVAILABLE, exception);
      }
      Throwable cause = exception.getCause();
      if (cause instanceof ArcusHttpException arcusHttp) {
        throw arcusHttp;
      }
      if (cause instanceof TimeoutException || cause instanceof OperationTimeoutException) {
        throw new ArcusHttpException(ErrorCode.ARCUS_TIMEOUT, cause);
      }
      if (cause instanceof CancellationException) {
        throw new ArcusHttpException(ErrorCode.ARCUS_UNAVAILABLE, cause);
      }
      throw new ArcusHttpException(ErrorCode.ARCUS_OPERATION_FAILED, cause);
    } catch (IllegalStateException exception) {
      throw new ArcusHttpException(ErrorCode.ARCUS_UNAVAILABLE, exception);
    } catch (IllegalArgumentException exception) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST, exception);
    }
  }

  public <T> CollectionOperationResult<T> collection(Supplier<CollectionFuture<T>> supplier) {
    final CollectionFuture<?>[] holder = new CollectionFuture<?>[1];
    T value = call(() -> {
      var future = supplier.get();
      holder[0] = future;
      return future;
    });
    var status = holder[0].getOperationStatus();
    if (status == null) {
      throw new ArcusHttpException(ErrorCode.ARCUS_OPERATION_FAILED);
    }
    String name = status.getResponse().name();
    // An empty retrieval or an absent Set element is a valid result, not an absent item.
    if (!status.isSuccess() && !(name.equals("NOT_FOUND_ELEMENT") && value instanceof Collection<?>)
        && !(name.equals("NOT_FOUND_ELEMENT") && value instanceof Map<?, ?>)
        && !name.equals("NOT_EXIST")) {
      reject(status);
    }
    return new CollectionOperationResult<>(name, value);
  }

  public void reject(OperationStatus status) {
    if (status == null) {
      throw new ArcusHttpException(ErrorCode.ARCUS_OPERATION_FAILED);
    }
    ErrorCode code = switch (statusCode(status)) {
      case ERR_NOT_FOUND -> ErrorCode.KEY_NOT_FOUND;
      case ERR_NOT_FOUND_ELEMENT -> ErrorCode.ELEMENT_NOT_FOUND;
      case ERR_EXISTS, ERR_ELEMENT_EXISTS -> ErrorCode.KEY_ALREADY_EXISTS;
      case ERR_TYPE_MISMATCH, ERR_BKEY_MISMATCH, ERR_EFLAG_MISMATCH ->
          ErrorCode.VALUE_TYPE_MISMATCH;
      case ERR_OUT_OF_RANGE, ERR_INVAL, ERR_ATTR_BAD_VALUE, ERR_NOTHING_TO_UPDATE ->
          ErrorCode.INVALID_REQUEST;
      case ERR_UNREADABLE, ERR_OVERFLOWED, ERR_ATTR_MISMATCH, ERR_NOT_STORED ->
          ErrorCode.COLLECTION_CONFLICT;
      case ERR_ATTR_NOT_FOUND -> ErrorCode.ATTRIBUTE_NOT_FOUND;
      case CANCELLED, INTERRUPTED -> ErrorCode.ARCUS_UNAVAILABLE;
      case TIMEDOUT -> ErrorCode.ARCUS_TIMEOUT;
      default -> ErrorCode.ARCUS_OPERATION_FAILED;
    };
    throw new ArcusHttpException(code);
  }

  private StatusCode statusCode(OperationStatus status) {
    if (status.getStatusCode() != null) {
      return status.getStatusCode();
    }

    String message = status.getMessage();
    if (message == null) {
      return StatusCode.UNDEFINED;
    }

    // Older OperationStatus constructors omit the code. Never return or log raw messages.
    for (StatusCode code : StatusCode.values()) {
      if (code.name().equals(message) || code.name().equals("ERR_" + message)) {
        return code;
      }
    }

    return switch (message) {
      case "ATTR_ERROR_NOT_FOUND" -> StatusCode.ERR_ATTR_NOT_FOUND;
      case "ATTR_ERROR_BAD_VALUE" -> StatusCode.ERR_ATTR_BAD_VALUE;
      case "CANCELED" -> StatusCode.CANCELLED;
      default -> StatusCode.UNDEFINED;
    };
  }

  public BulkOperationResult bulk(Map<String, ? extends OperationStatus> failures) {
    Map<String, String> result = new LinkedHashMap<>();
    failures.forEach((key, status)
        -> result.put(key, status instanceof CollectionOperationStatus collectionStatus
        ? collectionStatus.getResponse().name() : statusCode(status).name()));
    return new BulkOperationResult(true, result);
  }

  public PipedOperationResult piped(
      Supplier<CollectionFuture<Map<Integer, CollectionOperationStatus>>> supplier
  ) {
    return result(() -> {
      var future = supplier.get();
      var failures = future.get();
      var status = future.getOperationStatus();
      if (status == null) {
        throw new ArcusHttpException(ErrorCode.ARCUS_OPERATION_FAILED);
      }

      Map<Integer, String> mapped = new LinkedHashMap<>();
      failures.forEach((index, failure) -> mapped.put(index, failure.getResponse().name()));
      // A failed batch may prevent later batches from being submitted. Keep partial results.
      return new PipedOperationResult(status.getResponse().name(), status.isSuccess(), mapped);
    });
  }

  @FunctionalInterface
  public interface ResultSupplier<T> {

    T get() throws InterruptedException, ExecutionException;
  }

}
