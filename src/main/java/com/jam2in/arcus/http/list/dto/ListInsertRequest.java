package com.jam2in.arcus.http.list.dto;

import com.jam2in.arcus.http.common.dto.CreateAttributes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ListInsertRequest(
    @NotBlank
    String key,

    @NotNull
    Integer index,

    @NotNull
    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value,

    @Valid
    CreateAttributes attributesForCreate
) {

}
