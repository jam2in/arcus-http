package com.jam2in.arcus.http.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.stream.Stream;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

  @Bean
  public OpenApiCustomizer arcusHttpOpenApi() {
    return openApi -> {
      openApi.setInfo(new Info()
          .title("Arcus HTTP API")
          .description("Arcus의 캐시 기능을 HTTP API로 제공합니다.")
          .version("v0"));
      openApi.setTags(Stream.of("KV", "List", "Set", "Map", "B+Tree", "Item", "Cache")
          .map(name -> openApi.getTags().stream()
              .filter(tag -> name.equals(tag.getName()))
              .findFirst().orElseGet(() -> new Tag().name(name)))
          .toList());
    };
  }
}
