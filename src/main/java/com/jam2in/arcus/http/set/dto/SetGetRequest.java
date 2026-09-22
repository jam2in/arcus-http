package com.jam2in.arcus.http.set.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetGetRequest(
    @NotBlank
    String key,

    @NotNull
    @Min(0)
    Integer count,

    Boolean dropIfEmpty
) {

}
