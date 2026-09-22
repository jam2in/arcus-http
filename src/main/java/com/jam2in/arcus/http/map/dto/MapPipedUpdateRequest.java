package com.jam2in.arcus.http.map.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record MapPipedUpdateRequest(
    @NotBlank
    String key,

    @NotEmpty
    @Size(max = 500)
    Map<@NotBlank String, @NotNull Object> elements
) {

}
