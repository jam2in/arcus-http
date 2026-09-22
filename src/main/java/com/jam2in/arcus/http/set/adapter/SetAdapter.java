package com.jam2in.arcus.http.set.adapter;

import com.jam2in.arcus.http.common.adapter.ClientSupport;
import com.jam2in.arcus.http.common.adapter.CollectionAttributesMapper;
import com.jam2in.arcus.http.common.model.BulkOperationResult;
import com.jam2in.arcus.http.common.model.CollectionOperationResult;
import com.jam2in.arcus.http.common.model.CollectionSettings;
import com.jam2in.arcus.http.common.model.PipedOperationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.collection.ElementValueType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SetAdapter {

  private final ArcusClientPool client;
  private final ClientSupport support;
  private final CollectionAttributesMapper attributesMapper;

  public CollectionOperationResult<Boolean> create(
      String key, CollectionSettings attributes
  ) {
    return support.collection(() -> client.asyncSopCreate(key,
        ElementValueType.OTHERS,
        attributesMapper.toCreateAttributes(attributes)));
  }

  public CollectionOperationResult<Boolean> insert(
      String key, Object value, CollectionSettings attributesForCreate
  ) {
    return support.collection(() -> client.asyncSopInsert(key, value,
        attributesForCreate == null ? null
            : attributesMapper.toCreateAttributes(
                attributesForCreate)));
  }

  public BulkOperationResult insertBulk(
      List<String> keys, Object value, CollectionSettings attributesForCreate
  ) {
    return support.bulk(
        support.call(() -> client.asyncSopInsertBulk(keys, value,
            attributesForCreate == null ? null
                : attributesMapper.toCreateAttributes(
                    attributesForCreate))));
  }

  public CollectionOperationResult<List<Object>> get(
      String key, Integer count, Boolean dropIfEmpty
  ) {
    return read(key, count, dropIfEmpty, false);
  }

  public CollectionOperationResult<List<Object>> getAndDelete(
      String key, Integer count, Boolean dropIfEmpty
  ) {
    return read(key, count, dropIfEmpty, true);
  }

  private CollectionOperationResult<List<Object>> read(
      String key, Integer count, Boolean dropIfEmpty, boolean delete
  ) {
    var result = support.collection(
        () -> client.asyncSopGet(key, count, delete, dropIfEmpty));
    return new CollectionOperationResult<>(result.status(), new ArrayList<>(result.value()));
  }

  public CollectionOperationResult<Boolean> exist(
      String key, Object value, Boolean dropIfEmpty
  ) {
    return support.collection(() -> client.asyncSopExist(key, value));
  }

  public CollectionOperationResult<Boolean> delete(
      String key, Object value, Boolean dropIfEmpty
  ) {
    return support.collection(
        () -> client.asyncSopDelete(key, value, dropIfEmpty));
  }

  public PipedOperationResult pipedInsertBulk(
      String key, List<Object> values, CollectionSettings attributesForCreate
  ) {
    var attributes = attributesForCreate == null
        ? null : attributesMapper.toCreateAttributes(
        attributesForCreate);
    return support.piped(
        () -> client.asyncSopPipedInsertBulk(key, new ArrayList<>(values), attributes));
  }

  public CollectionOperationResult<Map<Object, Boolean>> pipedExistBulk(
      String key, List<Object> values
  ) {
    return support.collection(
        () -> client.asyncSopPipedExistBulk(key, new ArrayList<>(values)));
  }

}
