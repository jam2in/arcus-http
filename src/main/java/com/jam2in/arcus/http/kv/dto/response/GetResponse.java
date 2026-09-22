package com.jam2in.arcus.http.kv.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetResponse(
    @Schema(description = "조회한 키", example = "demo:hello")
    String key,

    @Schema(types = {"string", "number", "boolean", "object", "array"},
        description = "저장된 JSON 값", example = "arcus")
    Object value
) {

}
