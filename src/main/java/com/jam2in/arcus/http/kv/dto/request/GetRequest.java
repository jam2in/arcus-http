package com.jam2in.arcus.http.kv.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Get 요청 DTO
 */
public record GetRequest(
    @NotBlank
    @Size(max = 4000)
    @Schema(description = "Arcus 키. UTF-8 최대 4,000바이트", example = "demo:hello")
    String key
) {

}
