package com.jam2in.arcus.http.btree.dto;

import com.jam2in.arcus.http.common.dto.CreateAttributes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record BTreeCreateRequest(
    @NotBlank
    String key,

    @Valid
    CreateAttributes attributes
) {

}
