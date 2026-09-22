package com.jam2in.arcus.http.set.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SetPipedExistRequest(
    @NotBlank
    String key,

    @NotEmpty
    @Size(max = 500)
    List<@NotNull Object> values
) {

}
