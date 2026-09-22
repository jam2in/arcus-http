package com.jam2in.arcus.http.common.model;

import java.util.Map;

public record BulkOperationResult(
    boolean complete,
    Map<String, String> failures
) {

}
