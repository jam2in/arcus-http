package com.jam2in.arcus.http.list.service;

import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.IndexedResponse;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.common.model.CollectionKind;
import com.jam2in.arcus.http.common.validator.CollectionAttributesValidator;
import com.jam2in.arcus.http.list.adapter.ListAdapter;
import com.jam2in.arcus.http.list.dto.ListCreateRequest;
import com.jam2in.arcus.http.list.dto.ListInsertBulkRequest;
import com.jam2in.arcus.http.list.dto.ListInsertRequest;
import com.jam2in.arcus.http.list.dto.ListPipedInsertRequest;
import com.jam2in.arcus.http.list.dto.ListRangeRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListService {

  private final ListAdapter adapter;
  private final CollectionAttributesValidator attributesValidator;

  public CollectionResponse<Boolean> create(ListCreateRequest request) {
    var attributes = request.attributes() == null ? null : request.attributes().toSettings();
    attributesValidator.validateForCreate(attributes, CollectionKind.LIST);

    var result = adapter.create(request.key(), attributes);
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> insert(ListInsertRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.LIST);

    var result = adapter.insert(
        request.key(), request.index(), request.value(), attributesForCreate);
    return CollectionResponse.from(result);
  }

  public BulkResponse insertBulk(ListInsertBulkRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.LIST);

    var result = adapter.insertBulk(
        request.keys(), request.index(), request.value(), attributesForCreate);
    return BulkResponse.from(result);
  }

  public CollectionResponse<List<Object>> get(ListRangeRequest request) {
    if (Boolean.TRUE.equals(request.dropIfEmpty())) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.get(request.key(), request.from(), request.to(), request.dropIfEmpty());
    return CollectionResponse.from(result);
  }

  public CollectionResponse<List<Object>> getAndDelete(ListRangeRequest request) {
    var result = adapter.getAndDelete(
        request.key(), request.from(), request.to(), request.dropIfEmpty());
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> delete(ListRangeRequest request) {
    var result = adapter.delete(request.key(), request.from(), request.to(), request.dropIfEmpty());
    return CollectionResponse.from(result);
  }

  public IndexedResponse pipedInsertBulk(ListPipedInsertRequest request) {
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.LIST);

    var result = adapter.pipedInsertBulk(
        request.key(), request.index(), request.values(), attributesForCreate);
    return IndexedResponse.from(result);
  }

}
