package com.jam2in.arcus.http.advanced.flush.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FlushRequest(
    @Pattern(regexp = "(?:[a-zA-Z0-9_+.{}][a-zA-Z0-9_+.{}-]*)?")
    @Size(max = 3995)
    String prefix,

    @Min(0)
    Integer delay
) {

}
