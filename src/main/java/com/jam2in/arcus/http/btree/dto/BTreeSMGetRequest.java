package com.jam2in.arcus.http.btree.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record BTreeSMGetRequest(
    @NotEmpty
    @UniqueElements
    List<@NotBlank String> keys,

    @NotNull
    @Valid
    BKey from,

    @NotNull
    @Valid
    BKey to,

    @Valid
    EFlagFilter filter,

    @NotNull
    @Min(1)
    @Max(1000)
    Integer count,

    Boolean unique
) {

}
