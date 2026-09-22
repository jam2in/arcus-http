package com.jam2in.arcus.http.btree.controller;

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
import com.jam2in.arcus.http.btree.service.BTreeService;
import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.IndexedResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/btree")
@RequiredArgsConstructor
public class BTreeController implements BTreeControllerSpec {

  private final BTreeService service;

  @PostMapping("/create")
  public ApiResult<CollectionResponse<Boolean>> create(
      @Valid @RequestBody BTreeCreateRequest request
  ) {
    return ApiResult.success(service.create(request));
  }

  @PostMapping("/insert")
  public ApiResult<CollectionResponse<Boolean>> insert(
      @Valid @RequestBody BTreeStoreRequest request
  ) {
    return ApiResult.success(service.insert(request));
  }

  @PatchMapping("/upsert")
  public ApiResult<CollectionResponse<Boolean>> upsert(
      @Valid @RequestBody BTreeStoreRequest request
  ) {
    return ApiResult.success(service.upsert(request));
  }

  @PostMapping("/insert-and-get-trimmed")
  public ApiResult<CollectionResponse<BTreeTrimmedResponse>> insertAndGetTrimmed(
      @Valid @RequestBody BTreeStoreRequest request
  ) {
    return ApiResult.success(service.insertAndGetTrimmed(request));
  }

  @PatchMapping("/upsert-and-get-trimmed")
  public ApiResult<CollectionResponse<BTreeTrimmedResponse>> upsertAndGetTrimmed(
      @Valid @RequestBody BTreeStoreRequest request
  ) {
    return ApiResult.success(service.upsertAndGetTrimmed(request));
  }

  @PostMapping("/insert-bulk")
  public ApiResult<BulkResponse> insertBulk(@Valid @RequestBody BTreeInsertBulkRequest request) {
    return ApiResult.success(service.insertBulk(request));
  }

  @PatchMapping("/update")
  public ApiResult<CollectionResponse<Boolean>> update(
      @Valid @RequestBody BTreeUpdateRequest request
  ) {
    return ApiResult.success(service.update(request));
  }

  @PatchMapping("/incr")
  public ApiResult<CollectionResponse<String>> incr(@Valid @RequestBody BTreeCounterRequest request) {
    return ApiResult.success(service.incr(request));
  }

  @PatchMapping("/decr")
  public ApiResult<CollectionResponse<String>> decr(@Valid @RequestBody BTreeCounterRequest request) {
    return ApiResult.success(service.decr(request));
  }

  @PostMapping("/get")
  public ApiResult<CollectionResponse<List<BTreeElementResponse>>> get(
      @Valid @RequestBody BTreeGetRequest request
  ) {
    return ApiResult.success(service.get(request));
  }

  @PatchMapping("/get-and-delete")
  public ApiResult<CollectionResponse<List<BTreeElementResponse>>> getAndDelete(
      @Valid @RequestBody BTreeGetRequest request
  ) {
    return ApiResult.success(service.getAndDelete(request));
  }

  @DeleteMapping("/delete")
  public ApiResult<CollectionResponse<Boolean>> delete(
      @Valid @RequestBody BTreeDeleteRequest request
  ) {
    return ApiResult.success(service.delete(request));
  }

  @PostMapping("/get-item-count")
  public ApiResult<CollectionResponse<Integer>> getItemCount(
      @Valid @RequestBody BTreeCountRequest request
  ) {
    return ApiResult.success(service.getItemCount(request));
  }

  @PostMapping("/get-bulk")
  public ApiResult<Map<String, CollectionResponse<List<BTreeElementResponse>>>> getBulk(
      @Valid @RequestBody BTreeGetBulkRequest request
  ) {
    return ApiResult.success(service.getBulk(request));
  }

  @PostMapping("/smget")
  public ApiResult<CollectionResponse<BTreeSMGetResponse>> smget(
      @Valid @RequestBody BTreeSMGetRequest request
  ) {
    return ApiResult.success(service.smget(request));
  }

  @PostMapping("/find-position")
  public ApiResult<CollectionResponse<Integer>> findPosition(
      @Valid @RequestBody BTreePositionRequest request
  ) {
    return ApiResult.success(service.findPosition(request));
  }

  @PostMapping("/find-position-with-get")
  public ApiResult<CollectionResponse<Map<Integer, BTreeElementResponse>>> findPositionWithGet(
      @Valid @RequestBody BTreeAroundRequest request
  ) {
    return ApiResult.success(service.findPositionWithGet(request));
  }

  @PostMapping("/get-by-position")
  public ApiResult<CollectionResponse<Map<Integer, BTreeElementResponse>>> getByPosition(
      @Valid @RequestBody BTreeByPositionRequest request
  ) {
    return ApiResult.success(service.getByPosition(request));
  }

  @PostMapping("/piped-insert-bulk")
  public ApiResult<IndexedResponse> pipedInsertBulk(
      @Valid @RequestBody BTreePipedInsertRequest request
  ) {
    return ApiResult.success(service.pipedInsertBulk(request));
  }

  @PatchMapping("/piped-update-bulk")
  public ApiResult<IndexedResponse> pipedUpdateBulk(
      @Valid @RequestBody BTreePipedUpdateRequest request
  ) {
    return ApiResult.success(service.pipedUpdateBulk(request));
  }

}
