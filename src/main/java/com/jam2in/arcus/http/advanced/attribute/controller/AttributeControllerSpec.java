package com.jam2in.arcus.http.advanced.attribute.controller;

import com.jam2in.arcus.http.advanced.attribute.dto.AttributesResponse;
import com.jam2in.arcus.http.advanced.attribute.dto.GetAttributesRequest;
import com.jam2in.arcus.http.advanced.attribute.dto.SetAttributesRequest;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Item")
public interface AttributeControllerSpec {

  @Operation(operationId = "item_getAttributes", summary = "아이템 속성 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<AttributesResponse>> getAttributes(
      GetAttributesRequest request
  );

  @Operation(operationId = "item_setAttributes", summary = "아이템 속성 변경")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CollectionResponse<Boolean>> setAttributes(
      SetAttributesRequest request
  );

}
