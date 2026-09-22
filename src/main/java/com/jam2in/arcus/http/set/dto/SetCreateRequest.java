package com.jam2in.arcus.http.set.dto;

import com.jam2in.arcus.http.common.dto.CreateAttributes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record SetCreateRequest(
    @NotBlank
    String key,

    @Valid
    CreateAttributes attributes
) {

}
