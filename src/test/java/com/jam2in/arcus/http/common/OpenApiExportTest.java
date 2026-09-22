package com.jam2in.arcus.http.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Tag("api-docs")
@TestPropertySource(properties = "springdoc.writer-with-order-by-keys=true")
class OpenApiExportTest extends ClientHttpTest {

  @Autowired
  private JsonMapper mapper;

  @Value("${server.port}")
  private int serverPort;

  @Test
  @DisplayName("실제 Controller와 DTO의 OpenAPI 명세를 문서 생성용 파일로 내보낸다")
  void exportsApplicationSpecification() throws Exception {
    // given
    Path output = Path.of(System.getProperty("openapi.output"));

    // when
    String specification = mvc.perform(get("http://localhost:" + serverPort + "/v3/api-docs"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString(StandardCharsets.UTF_8);

    // then
    ObjectNode document = (ObjectNode) mapper.readTree(specification);
    assertThat(document.path("openapi").asString()).startsWith("3.");
    assertThat(document.path("paths").size()).isPositive();
    assertThat(document.path("components").path("schemas").size()).isPositive();

    document.path("components").path("schemas").forEach(this::expandSchemaTypes);

    Files.createDirectories(output.getParent());
    Files.writeString(output, mapper.writeValueAsString(document), StandardCharsets.UTF_8);
  }

  private void expandSchemaTypes(JsonNode schema) {
    if (!(schema instanceof ObjectNode object)) {
      return;
    }

    JsonNode types = object.get("type");
    if (types != null && types.isArray()) {
      // html2 loses object and array alternatives when normalizing a 3.1 type array.
      ObjectNode union = mapper.createObjectNode();
      var alternatives = union.putArray("anyOf");
      for (JsonNode type : types) {
        ObjectNode alternative = alternatives.addObject().put("type", type.asString());
        if ("array".equals(type.asString())) {
          alternative.set("items",
              object.has("items") ? object.get("items") : mapper.createObjectNode());
        }
      }
      object.remove("type");
      if (object.has("anyOf")) {
        object.withArray("allOf").add(union);
      } else {
        object.set("anyOf", alternatives);
      }
    }

    object.path("properties").forEach(this::expandSchemaTypes);
    for (String keyword : new String[] {"items", "additionalProperties", "not"}) {
      expandSchemaTypes(object.path(keyword));
    }
    for (String keyword : new String[] {"allOf", "anyOf", "oneOf", "prefixItems"}) {
      object.path(keyword).forEach(this::expandSchemaTypes);
    }
  }
}
