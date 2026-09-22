package com.jam2in.arcus.http.kv.controller;

import com.jam2in.arcus.http.common.dto.BooleanResponse;
import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import com.jam2in.arcus.http.kv.dto.request.AffixRequest;
import com.jam2in.arcus.http.kv.dto.request.CounterRequest;
import com.jam2in.arcus.http.kv.dto.request.DeleteBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.GetBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.GetRequest;
import com.jam2in.arcus.http.kv.dto.request.SetRequest;
import com.jam2in.arcus.http.kv.dto.request.StoreBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.TouchRequest;
import com.jam2in.arcus.http.kv.dto.response.CounterResponse;
import com.jam2in.arcus.http.kv.dto.response.GetBulkResponse;
import com.jam2in.arcus.http.kv.dto.response.GetResponse;
import com.jam2in.arcus.http.kv.dto.response.GetsBulkResponse;
import com.jam2in.arcus.http.kv.dto.response.GetsResponse;
import com.jam2in.arcus.http.kv.dto.response.SetResponse;
import com.jam2in.arcus.http.kv.service.KVService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kv")
@RequiredArgsConstructor
public class KVController implements KVControllerSpec {

  private final KVService service;

  @PostMapping("/set")
  public ApiResult<SetResponse> set(@Valid @RequestBody SetRequest request) {
    return ApiResult.success(service.set(request.key(), request.value(), request.expireTime()));
  }

  @PostMapping("/get")
  public ApiResult<GetResponse> get(@Valid @RequestBody GetRequest request) {
    return ApiResult.success(service.get(request.key()));
  }

  @PostMapping("/add")
  public ApiResult<SetResponse> add(@Valid @RequestBody SetRequest request) {
    return ApiResult.success(service.add(request));
  }

  @PatchMapping("/replace")
  public ApiResult<SetResponse> replace(@Valid @RequestBody SetRequest request) {
    return ApiResult.success(service.replace(request));
  }

  @PatchMapping("/append")
  public ApiResult<SetResponse> append(@Valid @RequestBody AffixRequest request) {
    return ApiResult.success(service.append(request));
  }

  @PatchMapping("/prepend")
  public ApiResult<SetResponse> prepend(@Valid @RequestBody AffixRequest request) {
    return ApiResult.success(service.prepend(request));
  }

  @PostMapping("/store-bulk")
  public ApiResult<BulkResponse> storeBulk(@Valid @RequestBody StoreBulkRequest request) {
    return ApiResult.success(service.storeBulk(request));
  }

  @PostMapping("/get-bulk")
  public ApiResult<GetBulkResponse> getBulk(@Valid @RequestBody GetBulkRequest request) {
    return ApiResult.success(service.getBulk(request));
  }

  @PostMapping("/gets")
  public ApiResult<GetsResponse> gets(@Valid @RequestBody GetRequest request) {
    return ApiResult.success(service.gets(request));
  }

  @PostMapping("/gets-bulk")
  public ApiResult<GetsBulkResponse> getsBulk(@Valid @RequestBody GetBulkRequest request) {
    return ApiResult.success(service.getsBulk(request));
  }

  @PatchMapping("/incr")
  public ApiResult<CounterResponse> incr(@Valid @RequestBody CounterRequest request) {
    return ApiResult.success(service.incr(request));
  }

  @PatchMapping("/decr")
  public ApiResult<CounterResponse> decr(@Valid @RequestBody CounterRequest request) {
    return ApiResult.success(service.decr(request));
  }

  @DeleteMapping("/delete")
  public ApiResult<BooleanResponse> delete(@Valid @RequestBody GetRequest request) {
    return ApiResult.success(service.delete(request));
  }

  @PatchMapping("/touch")
  public ApiResult<BooleanResponse> touch(@Valid @RequestBody TouchRequest request) {
    return ApiResult.success(service.touch(request));
  }

  @PatchMapping("/get-and-touch")
  public ApiResult<GetResponse> getAndTouch(@Valid @RequestBody TouchRequest request) {
    return ApiResult.success(service.getAndTouch(request));
  }

  @DeleteMapping("/delete-bulk")
  public ApiResult<BulkResponse> deleteBulk(@Valid @RequestBody DeleteBulkRequest request) {
    return ApiResult.success(service.deleteBulk(request));
  }

}
