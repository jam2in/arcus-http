package com.jam2in.arcus.http.common.exception;

public enum ErrorCode {

  INVALID_REQUEST("The request is invalid."),
  KEY_ALREADY_EXISTS("The item or element already exists."),
  ELEMENT_NOT_FOUND("The element does not exist."),
  ATTRIBUTE_NOT_FOUND("The attribute does not exist."),
  COLLECTION_CONFLICT("The operation conflicts with the item state."),
  KEY_NOT_FOUND("The key does not exist."),
  VALUE_TYPE_MISMATCH("The stored value is not a supported UTF-8 string."),
  ARCUS_OPERATION_FAILED("Arcus could not complete the operation."),
  ARCUS_UNAVAILABLE("Arcus is unavailable."),
  ARCUS_TIMEOUT("The Arcus operation timed out; its outcome may be unknown."),
  INTERNAL_ERROR("An unexpected error occurred.");

  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }

  public String message() {
    return message;
  }
}
