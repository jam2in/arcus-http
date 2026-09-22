package com.jam2in.arcus.http.common.dto;

import com.jam2in.arcus.http.common.model.PipedOperationResult;
import java.util.Map;

public record IndexedResponse(
    String status,
    boolean complete,
    Map<Integer, String> failures
) {

  public static IndexedResponse from(PipedOperationResult result) {
    return new IndexedResponse(result.status(), result.complete(), result.failures());
  }
}
