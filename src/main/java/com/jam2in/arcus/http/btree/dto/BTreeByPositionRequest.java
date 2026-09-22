package com.jam2in.arcus.http.btree.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BTreeByPositionRequest(
    @NotBlank
    String key,

    @NotNull
    BTreePositionRequest.Order order,

    @NotNull
    @Min(0)
    Integer from,

    @Min(0)
    Integer to
) {

}
