package com.jam2in.arcus.http.btree.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BTreeGetRequest(
    @NotBlank
    String key,

    @NotNull
    @Valid
    BKey from,

    @Valid
    BKey to,

    @Valid
    EFlagFilter filter,

    @Min(0)
    Integer offset,

    @Min(0)
    Integer count,

    Boolean dropIfEmpty
) {

}
