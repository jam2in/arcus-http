package com.jam2in.arcus.http.btree.dto;

import com.jam2in.arcus.http.btree.model.BTreeElement;
import io.swagger.v3.oas.annotations.media.Schema;

public record BTreeElementResponse(
    BKey bKey,
    String eFlag,
    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value
) {

  public static BTreeElementResponse from(BTreeElement element) {
    return element == null ? null
        : new BTreeElementResponse(BKey.from(element.bKey()), element.eFlag(), element.value());
  }
}
