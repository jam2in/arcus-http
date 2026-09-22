package com.jam2in.arcus.http.map.controller;

import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.IndexedResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import com.jam2in.arcus.http.map.dto.MapCreateRequest;
import com.jam2in.arcus.http.map.dto.MapDeleteRequest;
import com.jam2in.arcus.http.map.dto.MapGetRequest;
import com.jam2in.arcus.http.map.dto.MapInsertBulkRequest;
import com.jam2in.arcus.http.map.dto.MapInsertRequest;
import com.jam2in.arcus.http.map.dto.MapPipedInsertRequest;
import com.jam2in.arcus.http.map.dto.MapPipedUpdateRequest;
import com.jam2in.arcus.http.map.dto.MapUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;

@Tag(name = "Map")
public interface MapControllerSpec {

  @Operation(operationId = "map_create", summary = "Map 생성")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> create(MapCreateRequest request);

  @Operation(operationId = "map_insert", summary = "Map 요소 삽입")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> insert(MapInsertRequest request);

  @Operation(operationId = "map_insertBulk", summary = "여러 컬렉션에 요소 삽입")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<BulkResponse> insertBulk(MapInsertBulkRequest request);

  @Operation(operationId = "map_upsert", summary = "요소 삽입 또는 교체")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> upsert(MapInsertRequest request);

  @Operation(operationId = "map_update", summary = "기존 요소 변경")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> update(MapUpdateRequest request);

  @Operation(operationId = "map_get", summary = "요소 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Map<String, Object>>> get(MapGetRequest request);

  @Operation(operationId = "map_getAndDelete", summary = "요소 조회 후 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Map<String, Object>>> getAndDelete(
      MapGetRequest request);

  @Operation(operationId = "map_delete", summary = "요소 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> delete(MapDeleteRequest request);

  @Operation(operationId = "map_pipedInsertBulk", summary = "piped-insert-bulk")
  @ApiResponse(responseCode = "200", description = "일괄 처리 결과", useReturnTypeSchema = true)
  ApiResult<IndexedResponse> pipedInsertBulk(MapPipedInsertRequest request);

  @Operation(operationId = "map_pipedUpdateBulk", summary = "piped-update-bulk")
  @ApiResponse(responseCode = "200", description = "일괄 처리 결과", useReturnTypeSchema = true)
  ApiResult<IndexedResponse> pipedUpdateBulk(MapPipedUpdateRequest request);

}
