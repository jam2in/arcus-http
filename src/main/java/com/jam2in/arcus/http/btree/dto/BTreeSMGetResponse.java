package com.jam2in.arcus.http.btree.dto;

import com.jam2in.arcus.http.btree.model.SortMergeResult;
import java.util.List;
import java.util.Map;

public record BTreeSMGetResponse(
    List<Entry> elements,
    Map<String, String> missedKeys,
    List<TrimmedKey> trimmedKeys
) {

  public static BTreeSMGetResponse from(SortMergeResult result) {
    return new BTreeSMGetResponse(result.elements().stream()
        .map(entry -> new Entry(entry.key(), BTreeElementResponse.from(entry.element()))).toList(),
        result.missedKeys(), result.trimmedKeys().stream()
        .map(trimmed -> new TrimmedKey(trimmed.key(), BKey.from(trimmed.bKey()))).toList());
  }

  public record Entry(
      String key,
      BTreeElementResponse element
  ) {

  }

  public record TrimmedKey(
      String key,
      BKey bKey
  ) {

  }
}
