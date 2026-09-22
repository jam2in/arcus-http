package com.jam2in.arcus.http.common.dto;

import com.jam2in.arcus.http.common.model.CollectionSettings;
import com.jam2in.arcus.http.common.model.OverflowPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

public record UpdateAttributes(
    @Min(-1)
    @Schema(description = "만료 시간. -1은 sticky, 0은 만료 없음, 1~2592000은 상대 TTL(초), "
        + "2592000 초과는 만료 시각의 Unix timestamp(초)", example = "60")
    Integer expireTime,

    @Min(-1)
    Long maxCount,

    OverflowAction overflowAction,

    Boolean readable,

    @Min(0)
    Long maxBKeyRange,

    String maxBKeyRangeHex
) {

  public CollectionSettings toSettings() {
    return new CollectionSettings(expireTime, maxCount,
        overflowAction == null ? null : OverflowPolicy.valueOf(overflowAction.name()),
        readable, maxBKeyRange, maxBKeyRangeHex);
  }
}
