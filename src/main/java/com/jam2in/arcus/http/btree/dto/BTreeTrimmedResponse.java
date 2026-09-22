package com.jam2in.arcus.http.btree.dto;

import com.jam2in.arcus.http.btree.model.BTreeStoreResult;

public record BTreeTrimmedResponse(
    boolean stored,
    BTreeElementResponse trimmed
) {

  public static BTreeTrimmedResponse from(BTreeStoreResult result) {
    return new BTreeTrimmedResponse(result.stored(), BTreeElementResponse.from(result.trimmed()));
  }
}
