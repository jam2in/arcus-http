package com.jam2in.arcus.http.advanced.attribute.dto;

import jakarta.validation.constraints.NotBlank;

public record GetAttributesRequest(
    @NotBlank
    String key
) {

}
