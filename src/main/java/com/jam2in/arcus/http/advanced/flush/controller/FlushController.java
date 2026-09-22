package com.jam2in.arcus.http.advanced.flush.controller;

import com.jam2in.arcus.http.advanced.flush.dto.FlushRequest;
import com.jam2in.arcus.http.advanced.flush.service.FlushService;
import com.jam2in.arcus.http.common.dto.BooleanResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
public class FlushController implements FlushControllerSpec {

  private final FlushService service;

  @DeleteMapping("/flush")
  public ApiResult<BooleanResponse> flush(@Valid @RequestBody FlushRequest request) {
    return ApiResult.success(service.flush(request));
  }

}
