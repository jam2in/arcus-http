package com.jam2in.arcus.http.kv.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SetRequest(
    @NotBlank
    @Size(max = 4000)
    @Schema(description = "Arcus 키. UTF-8 최대 4,000바이트", example = "demo:hello")
    String key,

    @NotNull
    @Schema(types = {"string", "number", "boolean", "object", "array"},
        description = "JSON 값. 문자열, 숫자, 불리언, 객체와 배열 허용. 크기 제한은 기본 Transcoder 적용", example = "arcus")
    Object value,

    @NotNull
    @Min(-1)
    @Schema(description = "만료 시간. -1은 sticky, 0은 만료 없음, 1~2592000은 상대 TTL(초), "
        + "2592000 초과는 만료 시각의 Unix timestamp(초)", example = "60")
    Integer expireTime
) {

}
