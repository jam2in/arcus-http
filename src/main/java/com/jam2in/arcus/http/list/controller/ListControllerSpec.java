package com.jam2in.arcus.http.list.controller;

import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.IndexedResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import com.jam2in.arcus.http.list.dto.ListCreateRequest;
import com.jam2in.arcus.http.list.dto.ListInsertBulkRequest;
import com.jam2in.arcus.http.list.dto.ListInsertRequest;
import com.jam2in.arcus.http.list.dto.ListPipedInsertRequest;
import com.jam2in.arcus.http.list.dto.ListRangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "List")
public interface ListControllerSpec {

  @Operation(operationId = "list_create", summary = "List 생성")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> create(ListCreateRequest request);

  @Operation(operationId = "list_insert", summary = "List 요소 삽입")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> insert(ListInsertRequest request);

  @Operation(operationId = "list_insertBulk", summary = "여러 컬렉션에 요소 삽입")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<BulkResponse> insertBulk(ListInsertBulkRequest request);

  @Operation(operationId = "list_get", summary = "요소 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<List<Object>>> get(ListRangeRequest request);

  @Operation(operationId = "list_getAndDelete", summary = "요소 조회 후 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<List<Object>>> getAndDelete(
      ListRangeRequest request);

  @Operation(operationId = "list_delete", summary = "요소 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> delete(ListRangeRequest request);

  @Operation(operationId = "list_pipedInsertBulk", summary = "piped-insert-bulk")
  @ApiResponse(responseCode = "200", description = "일괄 처리 결과", useReturnTypeSchema = true)
  ApiResult<IndexedResponse> pipedInsertBulk(ListPipedInsertRequest request);

}
