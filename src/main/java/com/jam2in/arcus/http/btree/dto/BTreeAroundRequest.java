package com.jam2in.arcus.http.btree.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BTreeAroundRequest(
    @NotBlank
    String key,

    @NotNull
    @Valid
    BKey bKey,

    @NotNull
    BTreePositionRequest.Order order,

    @NotNull
    @Min(0)
    @Max(100)
    Integer count
) {

}
