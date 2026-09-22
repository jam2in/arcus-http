package com.jam2in.arcus.http.set.dto;

import com.jam2in.arcus.http.common.dto.CreateAttributes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SetPipedInsertRequest(
    @NotBlank
    String key,

    @NotEmpty
    List<@NotNull Object> values,

    @Valid
    CreateAttributes attributesForCreate
) {

}
