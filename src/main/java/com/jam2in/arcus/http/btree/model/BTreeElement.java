package com.jam2in.arcus.http.btree.model;

public record BTreeElement(
    BTreeKey bKey,
    String eFlag,
    Object value
) {

}
