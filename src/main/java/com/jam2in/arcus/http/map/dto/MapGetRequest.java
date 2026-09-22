package com.jam2in.arcus.http.map.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record MapGetRequest(
    @NotBlank
    String key,

    String mKey,

    List<@NotBlank String> mKeys,

    Boolean dropIfEmpty
) {

  public MapGetRequest {
    dropIfEmpty = Boolean.TRUE.equals(dropIfEmpty);
  }

}
