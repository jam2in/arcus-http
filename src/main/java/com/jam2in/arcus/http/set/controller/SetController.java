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
import com.jam2in.arcus.http.set.service.SetService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/set")
@RequiredArgsConstructor
public class SetController implements SetControllerSpec {

  private final SetService service;

  @PostMapping("/create")
  public ApiResult<CollectionResponse<Boolean>> create(@Valid @RequestBody SetCreateRequest request) {
    return ApiResult.success(service.create(request));
  }

  @PostMapping("/insert")
  public ApiResult<CollectionResponse<Boolean>> insert(@Valid @RequestBody SetInsertRequest request) {
    return ApiResult.success(service.insert(request));
  }

  @PostMapping("/insert-bulk")
  public ApiResult<BulkResponse> insertBulk(@Valid @RequestBody SetInsertBulkRequest request) {
    return ApiResult.success(service.insertBulk(request));
  }

  @PostMapping("/exist")
  public ApiResult<CollectionResponse<Boolean>> exist(@Valid @RequestBody SetValueRequest request) {
    return ApiResult.success(service.exist(request));
  }

  @PostMapping("/get")
  public ApiResult<CollectionResponse<List<Object>>> get(@Valid @RequestBody SetGetRequest request) {
    return ApiResult.success(service.get(request));
  }

  @PatchMapping("/get-and-delete")
  public ApiResult<CollectionResponse<List<Object>>> getAndDelete(
      @Valid @RequestBody SetGetRequest request
  ) {
    return ApiResult.success(service.getAndDelete(request));
  }

  @DeleteMapping("/delete")
  public ApiResult<CollectionResponse<Boolean>> delete(@Valid @RequestBody SetValueRequest request) {
    return ApiResult.success(service.delete(request));
  }

  @PostMapping("/piped-insert-bulk")
  public ApiResult<IndexedResponse> pipedInsertBulk(
      @Valid @RequestBody SetPipedInsertRequest request
  ) {
    return ApiResult.success(service.pipedInsertBulk(request));
  }

  @PostMapping("/piped-exist-bulk")
  public ApiResult<CollectionResponse<List<SetExistenceResponse>>> pipedExistBulk(
      @Valid @RequestBody SetPipedExistRequest request
  ) {
    return ApiResult.success(service.pipedExistBulk(request));
  }

}
