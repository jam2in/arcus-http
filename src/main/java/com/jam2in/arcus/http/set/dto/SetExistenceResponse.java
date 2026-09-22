package com.jam2in.arcus.http.set.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SetExistenceResponse(
    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value,
    boolean exists
) {

}
