package com.jam2in.arcus.http.map.adapter;

import com.jam2in.arcus.http.common.adapter.ClientSupport;
import com.jam2in.arcus.http.common.adapter.CollectionAttributesMapper;
import com.jam2in.arcus.http.common.model.BulkOperationResult;
import com.jam2in.arcus.http.common.model.CollectionOperationResult;
import com.jam2in.arcus.http.common.model.CollectionSettings;
import com.jam2in.arcus.http.common.model.PipedOperationResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.collection.ElementValueType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MapAdapter {

  private final ArcusClientPool client;
  private final ClientSupport support;
  private final CollectionAttributesMapper attributesMapper;

  public CollectionOperationResult<Boolean> create(
      String key, CollectionSettings attributes
  ) {
    return support.collection(() -> client.asyncMopCreate(
        key,
        ElementValueType.OTHERS,
        attributesMapper.toCreateAttributes(attributes))
    );
  }

  public CollectionOperationResult<Boolean> insert(
      String key, String mKey, Object value, CollectionSettings attributesForCreate
  ) {
    return support.collection(
        () -> client.asyncMopInsert(key, mKey, value,
            attributesForCreate == null ? null
                : attributesMapper.toCreateAttributes(
                    attributesForCreate)));
  }

  public BulkOperationResult insertBulk(
      List<String> keys, String mKey, Object value, CollectionSettings attributesForCreate
  ) {
    return support.bulk(
        support.call(() -> client.asyncMopInsertBulk(
            keys, mKey, value,
            attributesForCreate == null ? null
                : attributesMapper.toCreateAttributes(
                    attributesForCreate))));
  }

  public CollectionOperationResult<Map<String, Object>> get(
      String key, String mKey, List<String> mKeys, Boolean dropIfEmpty
  ) {
    return read(key, mKey, mKeys, dropIfEmpty, false);
  }

  public CollectionOperationResult<Map<String, Object>> getAndDelete(
      String key, String mKey, List<String> mKeys, Boolean dropIfEmpty
  ) {
    return read(key, mKey, mKeys, dropIfEmpty, true);
  }

  public CollectionOperationResult<Boolean> upsert(
      String key, String mKey, Object value, CollectionSettings attributesForCreate
  ) {
    return support.collection(
        () -> client.asyncMopUpsert(key, mKey, value,
            attributesForCreate == null ? null
                : attributesMapper.toCreateAttributes(
                    attributesForCreate)));
  }

  public CollectionOperationResult<Boolean> update(
      String key, String mKey, Object value
  ) {
    return support.collection(
        () -> client.asyncMopUpdate(key, mKey, value));
  }

  private CollectionOperationResult<Map<String, Object>> read(
      String key, String mKey, List<String> mKeys, Boolean dropIfEmpty, boolean delete
  ) {
    var result = support.collection(() -> mKey != null
        ? client.asyncMopGet(key, mKey, delete, dropIfEmpty)
        : mKeys != null
            ? client.asyncMopGet(key, mKeys, delete, dropIfEmpty)
            : client.asyncMopGet(key, delete, dropIfEmpty));
    return result;
  }

  public CollectionOperationResult<Boolean> delete(
      String key, String mKey, Boolean dropIfEmpty
  ) {
    return support.collection(() -> mKey == null
        ? client.asyncMopDelete(key, dropIfEmpty)
        : client.asyncMopDelete(key, mKey, dropIfEmpty));
  }

  public PipedOperationResult pipedInsertBulk(
      String key, Map<String, Object> elements, CollectionSettings attributesForCreate
  ) {
    var attributes = attributesForCreate == null
        ? null : attributesMapper.toCreateAttributes(
        attributesForCreate);
    return support.piped(() -> client.asyncMopPipedInsertBulk(
        key, new LinkedHashMap<String, Object>(elements), attributes));
  }

  public PipedOperationResult pipedUpdateBulk(
      String key, Map<String, Object> elements
  ) {
    return support.piped(() -> client.asyncMopPipedUpdateBulk(
        key, new LinkedHashMap<String, Object>(elements)));
  }

}
