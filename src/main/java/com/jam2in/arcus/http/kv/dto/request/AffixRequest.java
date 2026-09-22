package com.jam2in.arcus.http.kv.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AffixRequest(
    @NotBlank
    @Size(max = 4000)
    @Schema(description = "Arcus 키. UTF-8 최대 4,000바이트", example = "demo:hello")
    String key,

    @NotNull
    @Schema(description = "UTF-8 최대 1 MiB 문자열. 빈 문자열 허용", example = "arcus")
    String value
) {

}
