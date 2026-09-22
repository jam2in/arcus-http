package com.jam2in.arcus.http.btree.model;

import java.util.List;
import java.util.Map;

public record SortMergeResult(
    List<BTreeEntry> elements,
    Map<String, String> missedKeys,
    List<TrimmedKey> trimmedKeys
) {

}
