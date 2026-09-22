package com.jam2in.arcus.http.kv.dto.response;

import java.util.List;
import java.util.Map;

public record GetsBulkResponse(
    Map<String, GetsResponse> values,
    List<String> missingKeys
) {

}
