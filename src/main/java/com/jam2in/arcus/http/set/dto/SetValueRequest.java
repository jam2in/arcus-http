package com.jam2in.arcus.http.set.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetValueRequest(
    @NotBlank
    String key,

    @NotNull
    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value,

    Boolean dropIfEmpty
) {

  public SetValueRequest {
    dropIfEmpty = Boolean.TRUE.equals(dropIfEmpty);
  }

}
