package com.jam2in.arcus.http.list.adapter;

import com.jam2in.arcus.http.common.adapter.ClientSupport;
import com.jam2in.arcus.http.common.adapter.CollectionAttributesMapper;
import com.jam2in.arcus.http.common.model.BulkOperationResult;
import com.jam2in.arcus.http.common.model.CollectionOperationResult;
import com.jam2in.arcus.http.common.model.CollectionSettings;
import com.jam2in.arcus.http.common.model.PipedOperationResult;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.collection.ElementValueType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListAdapter {

  private final ArcusClientPool client;
  private final ClientSupport support;
  private final CollectionAttributesMapper attributesMapper;

  public CollectionOperationResult<Boolean> create(
      String key, CollectionSettings attributes
  ) {
    return support.collection(() -> client.asyncLopCreate(key,
        ElementValueType.OTHERS,
        attributesMapper.toCreateAttributes(attributes)));
  }

  public CollectionOperationResult<Boolean> insert(
      String key, Integer index, Object value, CollectionSettings attributesForCreate
  ) {
    return support.collection(
        () -> client.asyncLopInsert(key, index, value,
            attributesForCreate == null ? null
                : attributesMapper.toCreateAttributes(
                    attributesForCreate)));
  }

  public BulkOperationResult insertBulk(
      List<String> keys, Integer index, Object value, CollectionSettings attributesForCreate
  ) {
    return support.bulk(support.call(
        () -> client.asyncLopInsertBulk(keys, index, value,
            attributesForCreate == null ? null
                : attributesMapper.toCreateAttributes(
                    attributesForCreate))));
  }

  public CollectionOperationResult<List<Object>> get(
      String key, Integer from, Integer to, Boolean dropIfEmpty
  ) {
    return read(key, from, to, dropIfEmpty, false);
  }

  public CollectionOperationResult<List<Object>> getAndDelete(
      String key, Integer from, Integer to, Boolean dropIfEmpty
  ) {
    return read(key, from, to, dropIfEmpty, true);
  }

  private CollectionOperationResult<List<Object>> read(
      String key, Integer from, Integer to, Boolean dropIfEmpty, boolean delete
  ) {
    var result = support.collection(() -> to == null
        ? client.asyncLopGet(key, from, delete, dropIfEmpty)
        : client.asyncLopGet(key, from, to, delete,
            dropIfEmpty));
    return result;
  }

  public CollectionOperationResult<Boolean> delete(
      String key, Integer from, Integer to, Boolean dropIfEmpty
  ) {
    return support.collection(() -> to == null
        ? client.asyncLopDelete(key, from, dropIfEmpty)
        : client.asyncLopDelete(key, from, to,
            dropIfEmpty));
  }

  public PipedOperationResult pipedInsertBulk(
      String key, Integer index, List<Object> values, CollectionSettings attributesForCreate
  ) {
    var attributes = attributesForCreate == null
        ? null : attributesMapper.toCreateAttributes(
        attributesForCreate);
    return support.piped(() -> client.asyncLopPipedInsertBulk(
        key, index, new ArrayList<Object>(values), attributes));
  }

}
