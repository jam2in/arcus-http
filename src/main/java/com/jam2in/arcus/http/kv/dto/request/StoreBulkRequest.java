package com.jam2in.arcus.http.kv.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record StoreBulkRequest(
    @NotNull
    Mode mode,

    @NotNull
    @Min(-1)
    @Schema(description = "만료 시간. -1은 sticky, 0은 만료 없음, 1~2592000은 상대 TTL(초), "
        + "2592000 초과는 만료 시각의 Unix timestamp(초)", example = "60")
    Integer expireTime,

    List<@NotBlank String> keys,

    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value,

    Map<@NotBlank String, @NotNull Object> values
) {

  @AssertTrue(message = "keys/value 또는 values 중 하나를 지정해야 합니다")
  @JsonIgnore
  public boolean isSelectionValid() {
    return keys != null ? !keys.isEmpty() && value != null && values == null
        : value == null && values != null && !values.isEmpty();
  }

  public enum Mode {

    SET,
    ADD,
    REPLACE

  }
}
