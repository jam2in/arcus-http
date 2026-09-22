package com.jam2in.arcus.http.btree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record BTreeInsertElement(
    @NotNull
    @Valid
    BKey bKey,

    String eFlag,

    @NotNull
    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value
) {

}
