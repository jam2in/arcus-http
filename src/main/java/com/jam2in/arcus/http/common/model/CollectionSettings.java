package com.jam2in.arcus.http.common.model;

public record CollectionSettings(
    Integer expireTime,
    Long maxCount,
    OverflowPolicy overflowAction,
    Boolean readable,
    Long maxBKeyRange,
    String maxBKeyRangeHex
) {

}
