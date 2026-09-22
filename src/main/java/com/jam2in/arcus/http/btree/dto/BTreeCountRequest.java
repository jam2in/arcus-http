package com.jam2in.arcus.http.btree.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BTreeCountRequest(
    @NotBlank
    String key,

    @NotNull
    @Valid
    BKey from,

    @NotNull
    @Valid
    BKey to,

    @Valid
    EFlagFilter filter
) {

}
