package com.jam2in.arcus.http.advanced.attribute.model;

public record ItemAttributes(
    String type,
    Integer flags,
    Integer expireTime,
    Long count,
    Long maxCount,
    Boolean readable,
    String overflowAction,
    AttributeBKey maxBKeyRange,
    AttributeBKey minBKey,
    AttributeBKey maxBKey,
    Long trimmed
) {

}
