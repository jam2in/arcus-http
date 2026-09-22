package com.jam2in.arcus.http.advanced.flush.controller;

import com.jam2in.arcus.http.advanced.flush.dto.FlushRequest;
import com.jam2in.arcus.http.common.dto.BooleanResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Cache")
public interface FlushControllerSpec {

  @Operation(operationId = "cache_flush", summary = "전체 또는 prefix 아이템 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<BooleanResponse> flush(FlushRequest request);

}
