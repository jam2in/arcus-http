package com.jam2in.arcus.http.common.exception;

public class ArcusHttpException extends RuntimeException {

  private final ErrorCode code;

  public ArcusHttpException(ErrorCode code) {
    super(code.message());
    this.code = code;
  }

  public ArcusHttpException(ErrorCode code, Throwable cause) {
    super(code.message(), cause);
    this.code = code;
  }

  public ErrorCode code() {
    return code;
  }
}
