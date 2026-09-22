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
import com.jam2in.arcus.http.map.service.MapService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
public class MapController implements MapControllerSpec {

  private final MapService service;

  @PostMapping("/create")
  public ApiResult<CollectionResponse<Boolean>> create(@Valid @RequestBody MapCreateRequest request) {
    return ApiResult.success(service.create(request));
  }

  @PostMapping("/insert")
  public ApiResult<CollectionResponse<Boolean>> insert(@Valid @RequestBody MapInsertRequest request) {
    return ApiResult.success(service.insert(request));
  }

  @PostMapping("/insert-bulk")
  public ApiResult<BulkResponse> insertBulk(@Valid @RequestBody MapInsertBulkRequest request) {
    return ApiResult.success(service.insertBulk(request));
  }

  @PatchMapping("/upsert")
  public ApiResult<CollectionResponse<Boolean>> upsert(@Valid @RequestBody MapInsertRequest request) {
    return ApiResult.success(service.upsert(request));
  }

  @PatchMapping("/update")
  public ApiResult<CollectionResponse<Boolean>> update(@Valid @RequestBody MapUpdateRequest request) {
    return ApiResult.success(service.update(request));
  }

  @PostMapping("/get")
  public ApiResult<CollectionResponse<Map<String, Object>>> get(
      @Valid @RequestBody MapGetRequest request
  ) {
    return ApiResult.success(service.get(request));
  }

  @PatchMapping("/get-and-delete")
  public ApiResult<CollectionResponse<Map<String, Object>>> getAndDelete(
      @Valid @RequestBody MapGetRequest request
  ) {
    return ApiResult.success(service.getAndDelete(request));
  }

  @DeleteMapping("/delete")
  public ApiResult<CollectionResponse<Boolean>> delete(@Valid @RequestBody MapDeleteRequest request) {
    return ApiResult.success(service.delete(request));
  }

  @PostMapping("/piped-insert-bulk")
  public ApiResult<IndexedResponse> pipedInsertBulk(
      @Valid @RequestBody MapPipedInsertRequest request
  ) {
    return ApiResult.success(service.pipedInsertBulk(request));
  }

  @PatchMapping("/piped-update-bulk")
  public ApiResult<IndexedResponse> pipedUpdateBulk(
      @Valid @RequestBody MapPipedUpdateRequest request
  ) {
    return ApiResult.success(service.pipedUpdateBulk(request));
  }

}
