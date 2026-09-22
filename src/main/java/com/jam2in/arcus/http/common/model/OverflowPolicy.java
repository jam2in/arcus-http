package com.jam2in.arcus.http.common.model;

public enum OverflowPolicy {

  error,
  head_trim,
  tail_trim,
  smallest_trim,
  largest_trim,
  smallest_silent_trim,
  largest_silent_trim
}
