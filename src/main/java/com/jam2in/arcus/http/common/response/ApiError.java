package com.jam2in.arcus.http.common.response;

import com.jam2in.arcus.http.common.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API 오류 응답 클래스
 */
public record ApiError(
    @Schema(description = "안정적인 오류 식별자")
    ErrorCode code,

    @Schema(description = "오류 설명")
    String message
) {

  public static ApiError of(ErrorCode code) {
    return new ApiError(code, code.message());
  }
}
