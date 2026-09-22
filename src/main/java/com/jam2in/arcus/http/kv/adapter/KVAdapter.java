package com.jam2in.arcus.http.kv.adapter;

import com.jam2in.arcus.http.common.adapter.ClientSupport;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.common.model.BulkOperationResult;
import com.jam2in.arcus.http.kv.model.StoreMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.CASValue;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.ops.StoreType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KVAdapter {

  private final ArcusClientPool client;
  private final ClientSupport support;

  public boolean set(String key, Object value, int expireTime) {
    return store(() -> client.set(key, expireTime, value), ErrorCode.ARCUS_OPERATION_FAILED);
  }

  public Object get(String key) {
    return execute(() -> client.asyncGet(key));
  }

  private <T> T execute(Supplier<? extends Future<T>> operation) {
    return support.call(operation);
  }

  private boolean store(Supplier<OperationFuture<Boolean>> call, ErrorCode notStored) {
    return support.result(() -> {
      var future = call.get();
      boolean stored = Boolean.TRUE.equals(future.get());
      if (!stored) {
        var status = future.getStatus();
        if (status != null && status.getStatusCode() == StatusCode.ERR_NOT_STORED) {
          throw new ArcusHttpException(notStored);
        }
        support.reject(status);
      }
      return true;
    });
  }

  public boolean add(String key, Object value, int expireTime) {
    return store(() -> client.add(key, expireTime, value), ErrorCode.KEY_ALREADY_EXISTS);
  }

  public boolean replace(String key, Object value, int expireTime) {
    return store(() -> client.replace(key, expireTime, value), ErrorCode.KEY_NOT_FOUND);
  }

  public boolean append(String key, String value) {
    return store(() -> client.append(0, key, value), ErrorCode.KEY_NOT_FOUND);
  }

  public boolean prepend(String key, String value) {
    return store(() -> client.prepend(0, key, value), ErrorCode.KEY_NOT_FOUND);
  }

  public BulkOperationResult storeBulk(
      StoreMode mode, List<String> keys, int expireTime, Object value
  ) {
    return support.bulk(execute(() -> client.asyncStoreBulk(
        StoreType.valueOf(mode.name().toLowerCase(Locale.ROOT)), keys, expireTime, value)));
  }

  public BulkOperationResult storeBulk(
      StoreMode mode, Map<String, Object> values, int expireTime
  ) {
    Map<String, Object> clientValues = new LinkedHashMap<>(values);
    return support.bulk(execute(() -> client.asyncStoreBulk(
        StoreType.valueOf(mode.name().toLowerCase(Locale.ROOT)), clientValues, expireTime)));
  }

  public Map<String, Object> getBulk(List<String> keys) {
    return execute(() -> client.asyncGetBulk(keys));
  }

  public CASValue<Object> gets(String key) {
    return execute(() -> client.asyncGets(key));
  }

  public Map<String, CASValue<Object>> getsBulk(List<String> keys) {
    return execute(() -> client.asyncGetsBulk(keys));
  }

  public long counter(String key, int by, Long initial, Integer expireTime, boolean increment) {
    return support.result(() -> {
      OperationFuture<Long> future = initial == null
          ? (increment ? client.asyncIncr(key, by) : client.asyncDecr(key, by))
          : (increment ? client.asyncIncr(key, by, initial, expireTime)
              : client.asyncDecr(key, by, initial, expireTime));
      Long value = future.get();
      if (!future.getStatus().isSuccess()) {
        support.reject(future.getStatus());
      }
      return value;
    });
  }

  public boolean delete(String key) {
    return store(() -> client.delete(key), ErrorCode.KEY_NOT_FOUND);
  }

  public boolean touch(String key, int expireTime) {
    return support.result(() -> {
      var future = client.touch(key, expireTime);
      if (Boolean.TRUE.equals(future.get())) {
        return true;
      }
      if (future instanceof OperationFuture<?> operation) {
        support.reject(operation.getStatus());
      }
      throw new ArcusHttpException(ErrorCode.KEY_NOT_FOUND);
    });
  }

  public Object getAndTouch(String key, int expireTime) {
    return execute(() -> client.asyncGetAndTouch(key, expireTime));
  }

  public BulkOperationResult deleteBulk(List<String> keys) {
    return support.bulk(support.call(() -> client.asyncDeleteBulk(keys)));
  }

}
