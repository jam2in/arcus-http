package com.jam2in.arcus.http.kv.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record SetResponse(
    @Schema(description = "저장 성공 여부", example = "true")
    boolean stored
) {

}
