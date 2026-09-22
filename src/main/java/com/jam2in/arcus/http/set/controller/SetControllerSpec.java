package com.jam2in.arcus.http.set.controller;

import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.IndexedResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import com.jam2in.arcus.http.set.dto.SetCreateRequest;
import com.jam2in.arcus.http.set.dto.SetExistenceResponse;
import com.jam2in.arcus.http.set.dto.SetGetRequest;
import com.jam2in.arcus.http.set.dto.SetInsertBulkRequest;
import com.jam2in.arcus.http.set.dto.SetInsertRequest;
import com.jam2in.arcus.http.set.dto.SetPipedExistRequest;
import com.jam2in.arcus.http.set.dto.SetPipedInsertRequest;
import com.jam2in.arcus.http.set.dto.SetValueRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Set")
public interface SetControllerSpec {

  @Operation(operationId = "set_create", summary = "Set 생성")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> create(SetCreateRequest request);

  @Operation(operationId = "set_insert", summary = "Set 요소 삽입")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> insert(SetInsertRequest request);

  @Operation(operationId = "set_insertBulk", summary = "여러 컬렉션에 요소 삽입")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<BulkResponse> insertBulk(SetInsertBulkRequest request);

  @Operation(operationId = "set_exist", summary = "요소 존재 확인")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> exist(SetValueRequest request);

  @Operation(operationId = "set_get", summary = "요소 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<List<Object>>> get(SetGetRequest request);

  @Operation(operationId = "set_getAndDelete", summary = "요소 조회 후 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<List<Object>>> getAndDelete(SetGetRequest request);

  @Operation(operationId = "set_delete", summary = "요소 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> delete(SetValueRequest request);

  @Operation(operationId = "set_pipedInsertBulk", summary = "piped-insert-bulk")
  @ApiResponse(responseCode = "200", description = "일괄 처리 결과", useReturnTypeSchema = true)
  ApiResult<IndexedResponse> pipedInsertBulk(SetPipedInsertRequest request);

  @Operation(operationId = "set_pipedExistBulk", summary = "piped-exist-bulk")
  @ApiResponse(responseCode = "200", description = "일괄 처리 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<List<SetExistenceResponse>>> pipedExistBulk(
      SetPipedExistRequest request
  );

}
