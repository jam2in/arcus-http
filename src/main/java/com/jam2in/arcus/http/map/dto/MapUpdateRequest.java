package com.jam2in.arcus.http.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MapUpdateRequest(
    @NotBlank
    String key,

    @NotBlank
    String mKey,

    @NotNull
    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value
) {

}
