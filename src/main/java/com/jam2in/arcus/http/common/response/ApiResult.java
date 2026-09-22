package com.jam2in.arcus.http.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API 공통 응답 클래스
 */
public record ApiResult<T>(
    @Schema(description = "응답 코드. 0은 성공")
    int code,

    @Schema(description = "오류 설명. 성공 시 null")
    String message,

    @Schema(description = "응답 데이터. 데이터가 없으면 null")
    T content
) {

  public static <T> ApiResult<T> success(T content) {
    return new ApiResult<>(0, null, content);
  }

  public static <T> ApiResult<T> error(int code, String message) {
    return new ApiResult<>(code, message, null);
  }

  public static <T> ApiResult<T> error(int code, String message, T content) {
    return new ApiResult<>(code, message, content);
  }
}
