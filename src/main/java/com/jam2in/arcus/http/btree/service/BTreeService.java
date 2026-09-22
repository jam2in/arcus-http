package com.jam2in.arcus.http.btree.service;

import com.jam2in.arcus.http.btree.adapter.BTreeAdapter;
import com.jam2in.arcus.http.btree.dto.BTreeAroundRequest;
import com.jam2in.arcus.http.btree.dto.BTreeByPositionRequest;
import com.jam2in.arcus.http.btree.dto.BTreeCountRequest;
import com.jam2in.arcus.http.btree.dto.BTreeCounterRequest;
import com.jam2in.arcus.http.btree.dto.BTreeCreateRequest;
import com.jam2in.arcus.http.btree.dto.BTreeDeleteRequest;
import com.jam2in.arcus.http.btree.dto.BTreeElementResponse;
import com.jam2in.arcus.http.btree.dto.BTreeGetBulkRequest;
import com.jam2in.arcus.http.btree.dto.BTreeGetRequest;
import com.jam2in.arcus.http.btree.dto.BTreeInsertBulkRequest;
import com.jam2in.arcus.http.btree.dto.BTreePipedInsertRequest;
import com.jam2in.arcus.http.btree.dto.BTreePipedUpdateRequest;
import com.jam2in.arcus.http.btree.dto.BTreePositionRequest;
import com.jam2in.arcus.http.btree.dto.BTreeSMGetRequest;
import com.jam2in.arcus.http.btree.dto.BTreeSMGetResponse;
import com.jam2in.arcus.http.btree.dto.BTreeStoreRequest;
import com.jam2in.arcus.http.btree.dto.BTreeTrimmedResponse;
import com.jam2in.arcus.http.btree.dto.BTreeUpdateRequest;
import com.jam2in.arcus.http.btree.model.BTreeElement;
import com.jam2in.arcus.http.btree.model.BTreeElementUpdate;
import com.jam2in.arcus.http.btree.model.SortOrder;
import com.jam2in.arcus.http.btree.validator.BTreeInputValidator;
import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.IndexedResponse;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.common.model.CollectionKind;
import com.jam2in.arcus.http.common.model.CollectionOperationResult;
import com.jam2in.arcus.http.common.validator.CollectionAttributesValidator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BTreeService {

  private final BTreeAdapter adapter;
  private final CollectionAttributesValidator attributesValidator;
  private final BTreeInputValidator inputValidator;

  public CollectionResponse<Boolean> create(BTreeCreateRequest request) {
    var attributes = request.attributes() == null ? null : request.attributes().toSettings();
    attributesValidator.validateForCreate(attributes, CollectionKind.BTREE);

    var result = adapter.create(request.key(), attributes);
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> insert(BTreeStoreRequest request) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.BTREE);
    inputValidator.validateKey(bKey);
    inputValidator.validateHex(request.eFlag());

    var result = adapter.insert(
        request.key(), bKey, request.eFlag(), request.value(), attributesForCreate);
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Boolean> upsert(BTreeStoreRequest request) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.BTREE);
    inputValidator.validateKey(bKey);
    inputValidator.validateHex(request.eFlag());

    var result = adapter.upsert(
        request.key(), bKey, request.eFlag(), request.value(), attributesForCreate);
    return CollectionResponse.from(result);
  }

  public CollectionResponse<BTreeTrimmedResponse> insertAndGetTrimmed(
      BTreeStoreRequest request
  ) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.BTREE);
    inputValidator.validateKey(bKey);
    inputValidator.validateHex(request.eFlag());

    var result = adapter.insertAndGetTrimmed(
        request.key(), bKey, request.eFlag(), request.value(), attributesForCreate);
    return CollectionResponse.from(result, BTreeTrimmedResponse::from);
  }

  public CollectionResponse<BTreeTrimmedResponse> upsertAndGetTrimmed(
      BTreeStoreRequest request
  ) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.BTREE);
    inputValidator.validateKey(bKey);
    inputValidator.validateHex(request.eFlag());

    var result = adapter.upsertAndGetTrimmed(
        request.key(), bKey, request.eFlag(), request.value(), attributesForCreate);
    return CollectionResponse.from(result, BTreeTrimmedResponse::from);
  }

  public BulkResponse insertBulk(BTreeInsertBulkRequest request) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.BTREE);
    inputValidator.validateKey(bKey);
    inputValidator.validateHex(request.eFlag());

    var result = adapter.insertBulk(
        request.keys(), bKey, request.eFlag(), request.value(), attributesForCreate);
    return BulkResponse.from(result);
  }

  public CollectionResponse<Boolean> update(BTreeUpdateRequest request) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    var eFlagUpdate = request.eFlagUpdate() == null ? null : request.eFlagUpdate().toUpdate();
    inputValidator.validateKey(bKey);
    inputValidator.validateUpdate(request.value(), eFlagUpdate);

    var result = adapter.update(request.key(), bKey, request.value(), eFlagUpdate);
    return CollectionResponse.from(result);
  }

  public CollectionResponse<String> incr(BTreeCounterRequest request) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    inputValidator.validateKey(bKey);
    inputValidator.validateHex(request.eFlag());
    if (request.initial() == null && request.eFlag() != null) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.incr(
        request.key(), bKey, request.by(), request.initial(), request.eFlag());
    return CollectionResponse.from(result, Long::toUnsignedString);
  }

  public CollectionResponse<String> decr(BTreeCounterRequest request) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    inputValidator.validateKey(bKey);
    inputValidator.validateHex(request.eFlag());
    if (request.initial() == null && request.eFlag() != null) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.decr(
        request.key(), bKey, request.by(), request.initial(), request.eFlag());
    return CollectionResponse.from(result, Long::toUnsignedString);
  }

  public CollectionResponse<List<BTreeElementResponse>> get(BTreeGetRequest request) {
    var from = request.from() == null ? null : request.from().toKey();
    var to = request.to() == null ? null : request.to().toKey();
    var filter = request.filter() == null ? null : request.filter().toFilter();
    if (Boolean.TRUE.equals(request.dropIfEmpty())) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }
    inputValidator.validateRange(from, to);
    inputValidator.validateFilter(filter);
    if (to == null && ((request.offset() != null && request.offset() != 0)
        || (request.count() != null && request.count() != 0))) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.get(
        request.key(), from, to, filter, request.offset(), request.count(), request.dropIfEmpty());
    return CollectionResponse.from(result, this::toElements);
  }

  public CollectionResponse<List<BTreeElementResponse>> getAndDelete(BTreeGetRequest request) {
    var from = request.from() == null ? null : request.from().toKey();
    var to = request.to() == null ? null : request.to().toKey();
    var filter = request.filter() == null ? null : request.filter().toFilter();
    inputValidator.validateRange(from, to);
    inputValidator.validateFilter(filter);
    if (to == null && ((request.offset() != null && request.offset() != 0)
        || (request.count() != null && request.count() != 0))) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.getAndDelete(
        request.key(), from, to, filter, request.offset(), request.count(), request.dropIfEmpty());
    return CollectionResponse.from(result, this::toElements);
  }

  public CollectionResponse<Boolean> delete(BTreeDeleteRequest request) {
    var from = request.from() == null ? null : request.from().toKey();
    var to = request.to() == null ? null : request.to().toKey();
    var filter = request.filter() == null ? null : request.filter().toFilter();
    inputValidator.validateRange(from, to);
    inputValidator.validateFilter(filter);
    if (to == null && request.count() != null && request.count() != 0) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.delete(
        request.key(), from, to, filter, request.count(), request.dropIfEmpty());
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Integer> getItemCount(BTreeCountRequest request) {
    var from = request.from() == null ? null : request.from().toKey();
    var to = request.to() == null ? null : request.to().toKey();
    var filter = request.filter() == null ? null : request.filter().toFilter();
    inputValidator.validateRange(from, to);
    inputValidator.validateFilter(filter);

    var result = adapter.getItemCount(request.key(), from, to, filter);
    return CollectionResponse.from(result);
  }

  public Map<String, CollectionResponse<List<BTreeElementResponse>>> getBulk(
      BTreeGetBulkRequest request
  ) {
    var from = request.from() == null ? null : request.from().toKey();
    var to = request.to() == null ? null : request.to().toKey();
    var filter = request.filter() == null ? null : request.filter().toFilter();
    inputValidator.validateRange(from, to);
    inputValidator.validateFilter(filter);
    if (filter != null && filter.values().size() != 1) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }

    var result = adapter.getBulk(
        request.keys(), from, to, filter, request.offset(), request.count());
    return toBulkResponse(result);
  }

  public CollectionResponse<BTreeSMGetResponse> smget(BTreeSMGetRequest request) {
    var from = request.from() == null ? null : request.from().toKey();
    var to = request.to() == null ? null : request.to().toKey();
    var filter = request.filter() == null ? null : request.filter().toFilter();
    inputValidator.validateRange(from, to);
    inputValidator.validateFilter(filter);

    var result = adapter.smget(request.keys(), from, to, filter, request.count(), request.unique());
    return CollectionResponse.from(result, BTreeSMGetResponse::from);
  }

  public CollectionResponse<Integer> findPosition(BTreePositionRequest request) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    var order = SortOrder.valueOf(request.order().name());
    inputValidator.validateKey(bKey);

    var result = adapter.findPosition(request.key(), bKey, order);
    return CollectionResponse.from(result);
  }

  public CollectionResponse<Map<Integer, BTreeElementResponse>> findPositionWithGet(
      BTreeAroundRequest request
  ) {
    var bKey = request.bKey() == null ? null : request.bKey().toKey();
    var order = SortOrder.valueOf(request.order().name());
    inputValidator.validateKey(bKey);

    var result = adapter.findPositionWithGet(request.key(), bKey, order, request.count());
    return CollectionResponse.from(result, this::toPositions);
  }

  public CollectionResponse<Map<Integer, BTreeElementResponse>> getByPosition(
      BTreeByPositionRequest request
  ) {
    var order = SortOrder.valueOf(request.order().name());

    var result = adapter.getByPosition(request.key(), order, request.from(), request.to());
    return CollectionResponse.from(result, this::toPositions);
  }

  public IndexedResponse pipedInsertBulk(BTreePipedInsertRequest request) {
    var elements = request.elements().stream().map(element -> new BTreeElement(
        element.bKey().toKey(), element.eFlag(), element.value())).toList();
    var attributesForCreate = request.attributesForCreate() == null
        ? null : request.attributesForCreate().toSettings();
    attributesValidator.validateForCreate(attributesForCreate, CollectionKind.BTREE);
    var firstBKey = elements.getFirst().bKey();
    elements.forEach(element -> {
      inputValidator.validateRange(firstBKey, element.bKey());
      inputValidator.validateHex(element.eFlag());
    });

    var result = adapter.pipedInsertBulk(request.key(), elements, attributesForCreate);
    return IndexedResponse.from(result);
  }

  public IndexedResponse pipedUpdateBulk(BTreePipedUpdateRequest request) {
    var elements = request.elements().stream().map(element -> new BTreeElementUpdate(
        element.bKey().toKey(), element.value(),
        element.eFlagUpdate() == null ? null : element.eFlagUpdate().toUpdate())).toList();
    var firstBKey = elements.getFirst().bKey();
    elements.forEach(element -> {
      inputValidator.validateRange(firstBKey, element.bKey());
      inputValidator.validateUpdate(element.value(), element.eFlagUpdate());
    });

    var result = adapter.pipedUpdateBulk(request.key(), elements);
    return IndexedResponse.from(result);
  }

  private List<BTreeElementResponse> toElements(List<BTreeElement> elements) {
    return elements.stream().map(BTreeElementResponse::from).toList();
  }

  private Map<Integer, BTreeElementResponse> toPositions(Map<Integer, BTreeElement> elements) {
    Map<Integer, BTreeElementResponse> response = new LinkedHashMap<>();
    elements.forEach((position, element) ->
        response.put(position, BTreeElementResponse.from(element)));
    return response;
  }

  private Map<String, CollectionResponse<List<BTreeElementResponse>>> toBulkResponse(
      Map<String, CollectionOperationResult<List<BTreeElement>>> results
  ) {
    Map<String, CollectionResponse<List<BTreeElementResponse>>> response = new LinkedHashMap<>();
    results.forEach((key, result) ->
        response.put(key, CollectionResponse.from(result, this::toElements)));
    return response;
  }
}
