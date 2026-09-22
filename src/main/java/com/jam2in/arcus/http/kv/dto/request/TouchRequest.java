package com.jam2in.arcus.http.kv.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TouchRequest(
    @NotBlank
    String key,

    @NotNull
    @Min(-1)
    @Schema(description = "만료 시간. -1은 sticky, 0은 만료 없음, 1~2592000은 상대 TTL(초), "
        + "2592000 초과는 만료 시각의 Unix timestamp(초)", example = "60")
    Integer expireTime
) {

}
