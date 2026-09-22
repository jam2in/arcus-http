package com.jam2in.arcus.http.common.model;

public record CollectionOperationResult<T>(
    String status,
    T value
) {

}
