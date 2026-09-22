package com.jam2in.arcus.http.btree.dto;

import com.jam2in.arcus.http.btree.model.BitOperator;
import com.jam2in.arcus.http.btree.model.FlagUpdate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record EFlagUpdate(
    Boolean reset,

    String value,

    @Min(0)
    @Max(30)
    Integer offset,

    EFlagFilter.BitOp bitOp
) {

  public FlagUpdate toUpdate() {
    return new FlagUpdate(reset, value, offset,
        bitOp == null ? null : BitOperator.valueOf(bitOp.name()));
  }
}
