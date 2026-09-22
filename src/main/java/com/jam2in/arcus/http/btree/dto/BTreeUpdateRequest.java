package com.jam2in.arcus.http.btree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BTreeUpdateRequest(
    @NotBlank
    String key,

    @NotNull
    @Valid
    BKey bKey,

    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value,

    @Valid
    EFlagUpdate eFlagUpdate
) {

}
