package com.jam2in.arcus.http.kv.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CounterRequest(
    @NotBlank
    String key,

    @NotNull
    @Min(0)
    Integer by,

    @Min(0)
    Long initial,

    @Min(-1)
    @Schema(description = "만료 시간. -1은 sticky, 0은 만료 없음, 1~2592000은 상대 TTL(초), "
        + "2592000 초과는 만료 시각의 Unix timestamp(초)", example = "60")
    Integer expireTime
) {

  @AssertTrue(message = "initial과 expireTime은 함께 지정해야 합니다")
  @JsonIgnore
  public boolean isInitialValid() {
    return (initial == null) == (expireTime == null);
  }
}
