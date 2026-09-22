package com.jam2in.arcus.http.set.service;

import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.IndexedResponse;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.common.model.CollectionKind;
import com.jam2in.arcus.http.common.validator.CollectionAttributesValidator;
import com.jam2in.arcus.http.set.adapter.SetAdapter;
import com.jam2in.arcus.http.set.dto.SetCreateRequest;
import com.jam2in.arcus.http.set.dto.SetExistenceResponse;
import com.jam2in.arcus.http.set.dto.SetGetRequest;
import com.jam2in.arcus.http.set.dto.SetInsertBulkRequest;
import com.jam2in.arcus.http.set.dto.SetInsertRequest;
import com.jam2in.arcus.http.set.dto.SetPipedExistRequest;
import com.jam2in.arcus.http.set.dto.SetPipedInsertRequest;
import com.jam2in.arcus.http.set.dto.SetValueRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SetService {

  private final SetAdapter adapter;
  private final CollectionAttributesValidator attributesValidator;

  public CollectionResponse<Boolean> create(SetCreateRequest request) {
    var attributes = request.attributes() == null ? null : request.attributes().toSettings();
    attributesValidator.validateForCreate(attributes, CollectionKind.SET);

    var result = adapter.create(request.key(), attributes);
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> insert(SetInsertRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.SET);

    var result = adapter.insert(request.key(), request.value(), attributesForCreate);
    return CollectionResponse.from(result);
  }

  public BulkResponse insertBulk(SetInsertBulkRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.SET);

    var result = adapter.insertBulk(request.keys(), request.value(), attributesForCreate);
    return BulkResponse.from(result);
  }

  public CollectionResponse<List<Object>> get(SetGetRequest request) {
    if (Boolean.TRUE.equals(request.dropIfEmpty())) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.get(
        request.key(), request.count(), Boolean.TRUE.equals(request.dropIfEmpty()));
    return CollectionResponse.from(result);
  }

  public CollectionResponse<List<Object>> getAndDelete(SetGetRequest request) {
    var result = adapter.getAndDelete(
        request.key(), request.count(), Boolean.TRUE.equals(request.dropIfEmpty()));
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> exist(SetValueRequest request) {
    if (Boolean.TRUE.equals(request.dropIfEmpty())) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.exist(request.key(), request.value(), request.dropIfEmpty());
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> delete(SetValueRequest request) {
    var result = adapter.delete(request.key(), request.value(), request.dropIfEmpty());
    return CollectionResponse.from(result);
  }

  public IndexedResponse pipedInsertBulk(SetPipedInsertRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.SET);

    var result = adapter.pipedInsertBulk(request.key(), request.values(), attributesForCreate);
    return IndexedResponse.from(result);
  }

  public CollectionResponse<List<SetExistenceResponse>> pipedExistBulk(
      SetPipedExistRequest request
  ) {
    var result = adapter.pipedExistBulk(request.key(), request.values());
    var existence = request.values().stream()
        .map(value -> new SetExistenceResponse(value, result.value().get(value)))
        .toList();
    return new CollectionResponse<>(result.status(), existence);
  }

}
