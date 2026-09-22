package com.jam2in.arcus.http.advanced.attribute.controller;

import com.jam2in.arcus.http.advanced.attribute.dto.AttributesResponse;
import com.jam2in.arcus.http.advanced.attribute.dto.GetAttributesRequest;
import com.jam2in.arcus.http.advanced.attribute.dto.SetAttributesRequest;
import com.jam2in.arcus.http.advanced.attribute.service.AttributeService;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/item")
@RequiredArgsConstructor
public class AttributeController implements AttributeControllerSpec {

  private final AttributeService service;

  @PostMapping("/get-attr")
  public ApiResult<CollectionResponse<AttributesResponse>> getAttributes(
      @Valid @RequestBody GetAttributesRequest request
  ) {
    return ApiResult.success(service.getAttributes(request));
  }

  @PatchMapping("/set-attr")
  public ApiResult<CollectionResponse<Boolean>> setAttributes(
      @Valid @RequestBody SetAttributesRequest request
  ) {
    return ApiResult.success(service.setAttributes(request));
  }

}
