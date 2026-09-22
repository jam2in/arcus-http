package com.jam2in.arcus.http.advanced.attribute.dto;

import com.jam2in.arcus.http.advanced.attribute.model.AttributeBKey;
import com.jam2in.arcus.http.advanced.attribute.model.ItemAttributes;

public record AttributesResponse(
    String type,
    Integer flags,
    Integer expireTime,
    Long count,
    Long maxCount,
    Boolean readable,
    String overflowAction,
    BKeyValue maxBKeyRange,
    BKeyValue minBKey,
    BKeyValue maxBKey,
    Long trimmed
) {

  public static AttributesResponse from(ItemAttributes attributes) {
    return new AttributesResponse(
        attributes.type(), attributes.flags(), attributes.expireTime(),
        attributes.count(), attributes.maxCount(),
        attributes.readable(), attributes.overflowAction(),
        toBKey(attributes.maxBKeyRange()), toBKey(attributes.minBKey()),
        toBKey(attributes.maxBKey()), attributes.trimmed());
  }

  private static BKeyValue toBKey(AttributeBKey key) {
    return key == null ? null : new BKeyValue(key.number(), key.hex());
  }

  public record BKeyValue(
      Long number,
      String hex
  ) {

  }
}
