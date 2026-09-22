package com.jam2in.arcus.http.btree.dto;

import com.jam2in.arcus.http.btree.model.BitOperator;
import com.jam2in.arcus.http.btree.model.FlagComparison;
import com.jam2in.arcus.http.btree.model.FlagFilter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record EFlagFilter(
    @NotNull
    Compare compare,

    @NotEmpty
    @Size(max = 100)
    List<@NotNull String> values,

    @Min(0)
    @Max(30)
    Integer offset,

    BitOp bitOp,

    String bitValue
) {

  public FlagFilter toFilter() {
    return new FlagFilter(FlagComparison.valueOf(compare.name()), values, offset,
        bitOp == null ? null : BitOperator.valueOf(bitOp.name()), bitValue);
  }

  public enum Compare {

    Equal,

    NotEqual,

    LessThan,

    LessOrEqual,

    GreaterThan,

    GreaterOrEqual

  }

  public enum BitOp {

    AND,

    OR,

    XOR

  }
}
