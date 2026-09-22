package com.jam2in.arcus.http.btree.dto;

import com.jam2in.arcus.http.btree.model.BTreeKey;
import jakarta.validation.constraints.Min;

public record BKey(
    @Min(0)
    Long number,

    String hex
) {

  public static BKey from(BTreeKey key) {
    return key == null ? null : new BKey(key.number(), key.hex());
  }

  public BTreeKey toKey() {
    return new BTreeKey(number, hex);
  }
}
