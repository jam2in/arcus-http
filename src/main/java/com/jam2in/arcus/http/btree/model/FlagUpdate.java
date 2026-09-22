package com.jam2in.arcus.http.btree.model;

public record FlagUpdate(
    Boolean reset,
    String value,
    Integer offset,
    BitOperator bitOp
) {

}
