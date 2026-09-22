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
import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.IndexedResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;

@Tag(name = "B+Tree")
public interface BTreeControllerSpec {

  @Operation(operationId = "btree_create", summary = "빈 B+Tree 생성")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> create(BTreeCreateRequest request);

  @Operation(operationId = "btree_insert", summary = "B+Tree 요소 저장")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> insert(BTreeStoreRequest request);

  @Operation(operationId = "btree_upsert", summary = "B+Tree 요소 저장")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> upsert(BTreeStoreRequest request);

  @Operation(operationId = "btree_insertAndGetTrimmed", summary = "B+Tree 요소 저장")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<BTreeTrimmedResponse>> insertAndGetTrimmed(
      BTreeStoreRequest request
  );

  @Operation(operationId = "btree_upsertAndGetTrimmed", summary = "B+Tree 요소 저장")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<BTreeTrimmedResponse>> upsertAndGetTrimmed(
      BTreeStoreRequest request
  );

  @Operation(operationId = "btree_insertBulk", summary = "여러 B+Tree에 요소 삽입")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<BulkResponse> insertBulk(BTreeInsertBulkRequest request);

  @Operation(operationId = "btree_update", summary = "값과 EFlag 수정")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> update(BTreeUpdateRequest request);

  @Operation(operationId = "btree_incr", summary = "요소 값 증감")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<String>> incr(BTreeCounterRequest request);

  @Operation(operationId = "btree_decr", summary = "요소 값 증감")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<String>> decr(BTreeCounterRequest request);

  @Operation(operationId = "btree_get", summary = "bKey 범위 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<List<BTreeElementResponse>>> get(
      BTreeGetRequest request
  );

  @Operation(operationId = "btree_getAndDelete", summary = "조회 후 원자적 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<List<BTreeElementResponse>>> getAndDelete(
      BTreeGetRequest request
  );

  @Operation(operationId = "btree_delete", summary = "요소 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> delete(BTreeDeleteRequest request);

  @Operation(operationId = "btree_getItemCount", summary = "범위 내 요소 개수")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Integer>> getItemCount(BTreeCountRequest request);

  @Operation(operationId = "btree_getBulk", summary = "여러 B+Tree 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<Map<String, CollectionResponse<List<BTreeElementResponse>>>> getBulk(
      BTreeGetBulkRequest request
  );

  @Operation(operationId = "btree_smget", summary = "정렬 병합 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<BTreeSMGetResponse>> smget(
      BTreeSMGetRequest request
  );

  @Operation(operationId = "btree_findPosition", summary = "bKey 순위 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Integer>> findPosition(
      BTreePositionRequest request
  );

  @Operation(operationId = "btree_findPositionWithGet", summary = "순위와 주변 요소 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Map<Integer, BTreeElementResponse>>> findPositionWithGet(
      BTreeAroundRequest request
  );

  @Operation(operationId = "btree_getByPosition", summary = "위치 기준 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Map<Integer, BTreeElementResponse>>> getByPosition(
      BTreeByPositionRequest request
  );

  @Operation(operationId = "btree_pipedInsertBulk", summary = "piped-insert-bulk")
  @ApiResponse(responseCode = "200", description = "일괄 처리 결과", useReturnTypeSchema = true)
  ApiResult<IndexedResponse> pipedInsertBulk(BTreePipedInsertRequest request);

  @Operation(operationId = "btree_pipedUpdateBulk", summary = "piped-update-bulk")
  @ApiResponse(responseCode = "200", description = "일괄 처리 결과", useReturnTypeSchema = true)
  ApiResult<IndexedResponse> pipedUpdateBulk(BTreePipedUpdateRequest request);

}
