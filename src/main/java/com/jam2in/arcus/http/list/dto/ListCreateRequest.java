package com.jam2in.arcus.http.list.dto;

import com.jam2in.arcus.http.common.dto.CreateAttributes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * List 생성 요청 DTO
 */
public record ListCreateRequest(
    @NotBlank
    String key,

    @Valid
    CreateAttributes attributes
) {

}
