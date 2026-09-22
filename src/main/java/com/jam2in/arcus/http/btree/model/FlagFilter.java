package com.jam2in.arcus.http.btree.model;

import java.util.List;

public record FlagFilter(
    FlagComparison compare,
    List<String> values,
    Integer offset,
    BitOperator bitOp,
    String bitValue
) {

}
