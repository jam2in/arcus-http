package com.jam2in.arcus.http.advanced.attribute.dto;

import com.jam2in.arcus.http.common.dto.UpdateAttributes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SetAttributesRequest(
    @NotBlank
    String key,

    @NotNull
    @Valid
    UpdateAttributes attributes
) {

}
