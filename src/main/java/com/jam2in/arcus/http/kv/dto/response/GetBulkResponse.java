package com.jam2in.arcus.http.kv.dto.response;

import java.util.List;
import java.util.Map;

public record GetBulkResponse(
    Map<String, Object> values,
    List<String> missingKeys
) {

}
