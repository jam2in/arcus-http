package com.jam2in.arcus.http.common.exception;

import com.jam2in.arcus.http.common.response.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception exception,
      Object body,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request
  ) {
    if (status.value() == HttpStatus.NOT_ACCEPTABLE.value()) {
      return ResponseEntity.status(status).headers(headers).build();
    }

    ErrorCode code = status.is5xxServerError()
        ? ErrorCode.INTERNAL_ERROR
        : ErrorCode.INVALID_REQUEST;

    return super.handleExceptionInternal(exception,
        ApiResult.error(status.value(), code.message()),
        headers, status, request);
  }

  @ExceptionHandler(ArcusHttpException.class)
  public ResponseEntity<ApiResult<Void>> handleArcusHttp(ArcusHttpException exception) {
    HttpStatus status = switch (exception.code()) {
      case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
      case KEY_NOT_FOUND, ELEMENT_NOT_FOUND, ATTRIBUTE_NOT_FOUND -> HttpStatus.NOT_FOUND;
      case VALUE_TYPE_MISMATCH, KEY_ALREADY_EXISTS, COLLECTION_CONFLICT -> HttpStatus.CONFLICT;
      case ARCUS_OPERATION_FAILED -> HttpStatus.BAD_GATEWAY;
      case ARCUS_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
      case ARCUS_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
      case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
    return ResponseEntity.status(status)
        .body(ApiResult.error(status.value(), exception.code().message()));
  }


  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResult<Void>> handleUnexpected(Exception exception) {
    // Client exceptions can contain keys, values or connection details.
    log.error("Unhandled request failure of type {}", exception.getClass().getName());

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResult.error(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), ErrorCode.INTERNAL_ERROR.message()));
  }
}
