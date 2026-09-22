package com.jam2in.arcus.http.map.service;

import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.IndexedResponse;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.common.model.CollectionKind;
import com.jam2in.arcus.http.common.validator.CollectionAttributesValidator;
import com.jam2in.arcus.http.map.adapter.MapAdapter;
import com.jam2in.arcus.http.map.dto.MapCreateRequest;
import com.jam2in.arcus.http.map.dto.MapDeleteRequest;
import com.jam2in.arcus.http.map.dto.MapGetRequest;
import com.jam2in.arcus.http.map.dto.MapInsertBulkRequest;
import com.jam2in.arcus.http.map.dto.MapInsertRequest;
import com.jam2in.arcus.http.map.dto.MapPipedInsertRequest;
import com.jam2in.arcus.http.map.dto.MapPipedUpdateRequest;
import com.jam2in.arcus.http.map.dto.MapUpdateRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MapService {

  private final MapAdapter adapter;
  private final CollectionAttributesValidator attributesValidator;

  public CollectionResponse<Boolean> create(MapCreateRequest request) {
    var attributes = request.attributes() == null ? null : request.attributes().toSettings();
    attributesValidator.validateForCreate(attributes, CollectionKind.MAP);

    var result = adapter.create(request.key(), attributes);
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> insert(MapInsertRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.MAP);

    var result = adapter.insert(
        request.key(), request.mKey(), request.value(), attributesForCreate);
    return CollectionResponse.from(result);
  }

  public BulkResponse insertBulk(MapInsertBulkRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.MAP);

    var result = adapter.insertBulk(
        request.keys(), request.mKey(), request.value(), attributesForCreate);
    return BulkResponse.from(result);
  }

  public CollectionResponse<Map<String, Object>> get(MapGetRequest request) {
    if (Boolean.TRUE.equals(request.dropIfEmpty())) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }
    if ((request.mKey() != null && request.mKeys() != null)
        || (request.mKeys() != null && request.mKeys().isEmpty())) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.get(request.key(), request.mKey(), request.mKeys(), request.dropIfEmpty());
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Map<String, Object>> getAndDelete(MapGetRequest request) {
    if ((request.mKey() != null && request.mKeys() != null)
        || (request.mKeys() != null && request.mKeys().isEmpty())) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.getAndDelete(
        request.key(), request.mKey(), request.mKeys(), request.dropIfEmpty());
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> upsert(MapInsertRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.MAP);

    var result = adapter.upsert(
        request.key(), request.mKey(), request.value(), attributesForCreate);
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> update(MapUpdateRequest request) {
    var result = adapter.update(request.key(), request.mKey(), request.value());
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> delete(MapDeleteRequest request) {
    var result = adapter.delete(request.key(), request.mKey(), request.dropIfEmpty());
    return CollectionResponse.from(result);
  }

  public IndexedResponse pipedInsertBulk(MapPipedInsertRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.MAP);

    var result = adapter.pipedInsertBulk(request.key(), request.elements(), attributesForCreate);
    return IndexedResponse.from(result);
  }

  public IndexedResponse pipedUpdateBulk(MapPipedUpdateRequest request) {
    var result = adapter.pipedUpdateBulk(request.key(), request.elements());
    return IndexedResponse.from(result);
  }

}
