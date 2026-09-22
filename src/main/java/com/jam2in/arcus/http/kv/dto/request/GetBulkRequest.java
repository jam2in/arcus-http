package com.jam2in.arcus.http.kv.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GetBulkRequest(
    @NotNull
    List<@NotBlank String> keys
) {

}
