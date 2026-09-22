package com.jam2in.arcus.http.common.dto;

import com.jam2in.arcus.http.common.model.BulkOperationResult;
import java.util.Map;

public record BulkResponse(
    boolean complete,
    Map<String, String> failures
) {

  public static BulkResponse from(BulkOperationResult result) {
    return new BulkResponse(result.complete(), result.failures());
  }
}
