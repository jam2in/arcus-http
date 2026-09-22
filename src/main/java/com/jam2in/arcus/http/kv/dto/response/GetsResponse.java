package com.jam2in.arcus.http.kv.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetsResponse(
    String key,
    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value,
    long cas
) {

}
