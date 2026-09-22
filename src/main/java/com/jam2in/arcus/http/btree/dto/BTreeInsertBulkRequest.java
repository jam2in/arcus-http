package com.jam2in.arcus.http.btree.dto;

import com.jam2in.arcus.http.common.dto.CreateAttributes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record BTreeInsertBulkRequest(
    @NotEmpty
    @UniqueElements
    List<@NotBlank String> keys,

    @NotNull
    @Valid
    BKey bKey,

    String eFlag,

    @NotNull
    @Schema(types = {"string", "number", "boolean", "object", "array"})
    Object value,

    @Valid
    CreateAttributes attributesForCreate
) {

}
