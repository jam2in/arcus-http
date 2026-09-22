package com.jam2in.arcus.http.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

class ApiDocumentationTest extends ClientHttpTest {

  @Autowired
  private JsonMapper mapper;

  @Test
  @DisplayName("전체 API가 합의한 HTTP 메서드와 요청·응답 스키마로 Swagger에 노출된다")
  void documentsAllOperationsAndSchemas() throws Exception {
    // given
    var groups = Map.of(
        "kv", Map.of("post", "set add store-bulk get get-bulk gets gets-bulk", "patch", "replace append prepend incr decr touch get-and-touch", "delete", "delete delete-bulk"),
        "list", Map.of("post", "create insert insert-bulk piped-insert-bulk get", "patch", "get-and-delete", "delete", "delete"),
        "set", Map.of("post", "create insert insert-bulk piped-insert-bulk piped-exist-bulk get exist", "patch", "get-and-delete", "delete", "delete"),
        "map", Map.of("post", "create insert insert-bulk piped-insert-bulk get", "patch", "upsert update piped-update-bulk get-and-delete", "delete", "delete"),
        "btree", Map.of("post", "create insert insert-and-get-trimmed insert-bulk piped-insert-bulk get get-item-count get-bulk smget find-position find-position-with-get get-by-position", "patch", "upsert upsert-and-get-trimmed update piped-update-bulk incr decr get-and-delete", "delete", "delete"),
        "item", Map.of("post", "get-attr", "patch", "set-attr"),
        "cache", Map.of("delete", "flush"));

    // when
    var response = mvc.perform(get("/v3/api-docs"));

    // then
    response.andExpect(status().isOk());
    var document = mapper.readTree(response.andReturn().getResponse().getContentAsString());
    var tags = new ArrayList<String>();
    document.path("tags").forEach(tag -> tags.add(tag.path("name").asString()));
    assertThat(tags).containsExactly("KV", "List", "Set", "Map", "B+Tree", "Item", "Cache");
    var paths = document.path("paths");
    var operationIds = new HashSet<String>();
    int count = 0;
    for (var group : groups.entrySet()) {
      for (var methods : group.getValue().entrySet()) {
        for (String operation : methods.getValue().split(" ")) {
          String path = "/" + group.getKey() + "/" + operation;
          var endpoint = paths.path(path);
          assertThat(endpoint.has(methods.getKey())).as(path).isTrue();
          assertThat(endpoint.size()).as(path + " method count").isEqualTo(1);
          var method = endpoint.path(methods.getKey());
          String operationId = method.path("operationId").asString();
          assertThat(operationId).as(path).matches(group.getKey() + "_[a-z][a-zA-Z]*");
          assertThat(operationIds.add(operationId)).as(path + " unique operationId").isTrue();
          assertThat(method.path("requestBody").path("required").asBoolean()).as(path).isTrue();
          assertThat(method.path("responses").path("200").path("content").path("application/json").has("schema")).as(path).isTrue();
          count++;
        }
      }
    }
    assertThat(count).isEqualTo(65);
    assertThat(paths.size()).isEqualTo(count);
  }
}
