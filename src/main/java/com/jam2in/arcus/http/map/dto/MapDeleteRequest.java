package com.jam2in.arcus.http.map.dto;

import jakarta.validation.constraints.NotBlank;

public record MapDeleteRequest(
    @NotBlank
    String key,

    String mKey,

    Boolean dropIfEmpty
) {

  public MapDeleteRequest {
    dropIfEmpty = Boolean.TRUE.equals(dropIfEmpty);
  }

}
