package com.jam2in.arcus.http.btree.model;

public record BTreeElementUpdate(
    BTreeKey bKey,
    Object value,
    FlagUpdate eFlagUpdate
) {

}
