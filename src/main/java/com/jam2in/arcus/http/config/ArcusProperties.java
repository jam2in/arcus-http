package com.jam2in.arcus.http.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("arcus")
public record ArcusProperties(
    @NotBlank
    String zookeeperAddress,

    @NotBlank
    String serviceCode,

    @Min(1)
    int poolSize,

    @Min(1)
    @Max(60000)
    int operationTimeoutMs
) {

}
