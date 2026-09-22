package com.jam2in.arcus.http.common.model;

import java.util.Map;

public record PipedOperationResult(
    String status,
    boolean complete,
    Map<Integer, String> failures
) {

}
