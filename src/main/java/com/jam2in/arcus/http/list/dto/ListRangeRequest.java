package com.jam2in.arcus.http.list.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ListRangeRequest(
    @NotBlank
    String key,

    @NotNull
    Integer from,

    Integer to,

    Boolean dropIfEmpty
) {

  public ListRangeRequest {
    dropIfEmpty = Boolean.TRUE.equals(dropIfEmpty);
  }

}
