package com.jam2in.arcus.http.kv.service;

import com.jam2in.arcus.http.common.dto.BooleanResponse;
import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.kv.adapter.KVAdapter;
import com.jam2in.arcus.http.kv.dto.request.AffixRequest;
import com.jam2in.arcus.http.kv.dto.request.CounterRequest;
import com.jam2in.arcus.http.kv.dto.request.DeleteBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.GetBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.GetRequest;
import com.jam2in.arcus.http.kv.dto.request.SetRequest;
import com.jam2in.arcus.http.kv.dto.request.StoreBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.TouchRequest;
import com.jam2in.arcus.http.kv.dto.response.CounterResponse;
import com.jam2in.arcus.http.kv.dto.response.GetBulkResponse;
import com.jam2in.arcus.http.kv.dto.response.GetResponse;
import com.jam2in.arcus.http.kv.dto.response.GetsBulkResponse;
import com.jam2in.arcus.http.kv.dto.response.GetsResponse;
import com.jam2in.arcus.http.kv.dto.response.SetResponse;
import com.jam2in.arcus.http.kv.model.StoreMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.CASValue;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KVService {

  private final KVAdapter adapter;

  public SetResponse set(String key, Object value, int expireTime) {
    if (!adapter.set(key, value, expireTime)) {
      throw new ArcusHttpException(ErrorCode.ARCUS_OPERATION_FAILED);
    }

    return new SetResponse(true);
  }

  public GetResponse get(String key) {
    Object value = adapter.get(key);
    if (value == null) {
      throw new ArcusHttpException(ErrorCode.KEY_NOT_FOUND);
    }

    return new GetResponse(key, value);
  }

  public SetResponse add(SetRequest request) {
    return new SetResponse(adapter.add(request.key(), request.value(), request.expireTime()));
  }

  public SetResponse replace(SetRequest request) {
    return new SetResponse(adapter.replace(request.key(), request.value(), request.expireTime()));
  }

  public SetResponse append(AffixRequest request) {
    return new SetResponse(adapter.append(request.key(), request.value()));
  }

  public SetResponse prepend(AffixRequest request) {
    return new SetResponse(adapter.prepend(request.key(), request.value()));
  }

  public BulkResponse storeBulk(StoreBulkRequest request) {
    StoreMode mode = StoreMode.valueOf(request.mode().name());
    var result = request.keys() != null
        ? adapter.storeBulk(mode, request.keys(), request.expireTime(), request.value())
        : adapter.storeBulk(mode, request.values(), request.expireTime());

    return BulkResponse.from(result);
  }

  public GetBulkResponse getBulk(GetBulkRequest request) {
    Map<String, Object> values = adapter.getBulk(request.keys());
    return new GetBulkResponse(values, missingKeys(request.keys(), values));
  }

  public GetsResponse gets(GetRequest request) {
    CASValue<Object> value = adapter.gets(request.key());
    if (value == null) {
      throw new ArcusHttpException(ErrorCode.KEY_NOT_FOUND);
    }

    return new GetsResponse(request.key(), value.getValue(), value.getCas());
  }

  public GetsBulkResponse getsBulk(GetBulkRequest request) {
    Map<String, CASValue<Object>> values = adapter.getsBulk(request.keys());
    Map<String, GetsResponse> result = new LinkedHashMap<>();

    for (Map.Entry<String, CASValue<Object>> entry : values.entrySet()) {
      String entryKey = entry.getKey();
      CASValue<Object> entryValue = entry.getValue();

      result.put(entryKey,
          new GetsResponse(entryKey, entryValue.getValue(), entryValue.getCas()));
    }

    return new GetsBulkResponse(result, missingKeys(request.keys(), values));
  }

  public CounterResponse incr(CounterRequest request) {
    long value = adapter.counter(
        request.key(), request.by(), request.initial(), request.expireTime(), true);
    return new CounterResponse(request.key(), Long.toUnsignedString(value));
  }

  public CounterResponse decr(CounterRequest request) {
    long value = adapter.counter(
        request.key(), request.by(), request.initial(), request.expireTime(), false);
    return new CounterResponse(request.key(), Long.toUnsignedString(value));
  }

  public BooleanResponse delete(GetRequest request) {
    return new BooleanResponse(adapter.delete(request.key()));
  }

  public BooleanResponse touch(TouchRequest request) {
    return new BooleanResponse(adapter.touch(request.key(), request.expireTime()));
  }

  public GetResponse getAndTouch(TouchRequest request) {
    Object value = adapter.getAndTouch(request.key(), request.expireTime());
    if (value == null) {
      throw new ArcusHttpException(ErrorCode.KEY_NOT_FOUND);
    }

    return new GetResponse(request.key(), value);
  }

  public BulkResponse deleteBulk(DeleteBulkRequest request) {
    return BulkResponse.from(adapter.deleteBulk(request.keys()));
  }

  private List<String> missingKeys(List<String> keys, Map<String, ?> values) {
    return keys.stream()
        .distinct()
        .filter(key -> !values.containsKey(key))
        .toList();
  }

}
