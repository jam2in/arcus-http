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
import com.jam2in.arcus.http.list.service.ListService;
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
@RequestMapping("/list")
@RequiredArgsConstructor
public class ListController implements ListControllerSpec {

  private final ListService service;

  @PostMapping("/create")
  public ApiResult<CollectionResponse<Boolean>> create(
      @Valid @RequestBody ListCreateRequest request
  ) {
    return ApiResult.success(service.create(request));
  }

  @PostMapping("/insert")
  public ApiResult<CollectionResponse<Boolean>> insert(
      @Valid @RequestBody ListInsertRequest request
  ) {
    return ApiResult.success(service.insert(request));
  }

  @PostMapping("/insert-bulk")
  public ApiResult<BulkResponse> insertBulk(@Valid @RequestBody ListInsertBulkRequest request) {
    return ApiResult.success(service.insertBulk(request));
  }

  @PostMapping("/get")
  public ApiResult<CollectionResponse<List<Object>>> get(
      @Valid @RequestBody ListRangeRequest request
  ) {
    return ApiResult.success(service.get(request));
  }

  @PatchMapping("/get-and-delete")
  public ApiResult<CollectionResponse<List<Object>>> getAndDelete(
      @Valid @RequestBody ListRangeRequest request
  ) {
    return ApiResult.success(service.getAndDelete(request));
  }

  @DeleteMapping("/delete")
  public ApiResult<CollectionResponse<Boolean>> delete(@Valid @RequestBody ListRangeRequest request) {
    return ApiResult.success(service.delete(request));
  }

  @PostMapping("/piped-insert-bulk")
  public ApiResult<IndexedResponse> pipedInsertBulk(
      @Valid @RequestBody ListPipedInsertRequest request
  ) {
    return ApiResult.success(service.pipedInsertBulk(request));
  }

}
