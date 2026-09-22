package com.jam2in.arcus.http.kv.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record DeleteBulkRequest(
    @NotEmpty
    List<@NotBlank String> keys
) {

}
