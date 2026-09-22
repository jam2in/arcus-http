package com.jam2in.arcus.http.btree.model;

public record BTreeStoreResult(
    boolean stored,
    BTreeElement trimmed
) {

}
