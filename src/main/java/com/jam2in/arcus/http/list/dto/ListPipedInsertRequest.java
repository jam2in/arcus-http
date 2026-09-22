package com.jam2in.arcus.http.list.dto;

import com.jam2in.arcus.http.common.dto.CreateAttributes;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ListPipedInsertRequest(
    @NotBlank
    String key,

    @NotNull
    Integer index,

    @NotEmpty
    List<@NotNull Object> values,

    @Valid
    CreateAttributes attributesForCreate
) {

}
