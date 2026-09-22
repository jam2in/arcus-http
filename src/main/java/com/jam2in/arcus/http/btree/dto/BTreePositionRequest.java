package com.jam2in.arcus.http.btree.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BTreePositionRequest(
    @NotBlank
    String key,

    @NotNull
    @Valid
    BKey bKey,

    @NotNull
    Order order
) {

  public enum Order {

    ASC,

    DESC

  }
}
