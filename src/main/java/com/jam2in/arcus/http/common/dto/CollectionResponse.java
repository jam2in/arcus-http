package com.jam2in.arcus.http.common.dto;

import com.jam2in.arcus.http.common.model.CollectionOperationResult;
import java.util.function.Function;

public record CollectionResponse<T>(
    String status,
    T value
) {

  public static <T> CollectionResponse<T> from(CollectionOperationResult<T> result) {
    return new CollectionResponse<>(result.status(), result.value());
  }

  public static <T, R> CollectionResponse<R> from(
      CollectionOperationResult<T> result, Function<T, R> valueMapper
  ) {
    return new CollectionResponse<>(result.status(), valueMapper.apply(result.value()));
  }
}
