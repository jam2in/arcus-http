package com.jam2in.arcus.http.btree.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BTreeCounterRequest(
    @NotBlank
    String key,

    @NotNull
    @Valid
    BKey bKey,

    @NotNull
    @Min(0)
    Integer by,

    @Min(0)
    Long initial,

    String eFlag
) {

}
