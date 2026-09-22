package com.jam2in.arcus.http.common;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

class ObjectValueHttpTest extends ClientHttpTest {

  @ParameterizedTest
  @MethodSource("nullValues")
  @DisplayName("저장 원소 자체가 null인 요청은 클라이언트 호출 전에 거부한다")
  void rejectsNullValues(String path, String json) throws Exception {
    // given: 단일 저장과 일괄 저장의 null 원소 요청을 제공한다.

    // expected
    mvc.perform(post(path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(client);
  }

  static Stream<Arguments> nullValues() {
    return Stream.of(
        Arguments.of("/kv/set", "{\"key\":\"k\",\"value\":null,\"expireTime\":60}"),
        Arguments.of("/kv/store-bulk",
            "{\"mode\":\"SET\",\"expireTime\":60,\"values\":{\"k\":null}}"),
        Arguments.of("/list/insert", "{\"key\":\"k\",\"index\":0,\"value\":null}"),
        Arguments.of("/set/insert", "{\"key\":\"k\",\"value\":null}"),
        Arguments.of("/map/insert", "{\"key\":\"k\",\"mKey\":\"m\",\"value\":null}"),
        Arguments.of("/btree/insert",
            "{\"key\":\"k\",\"bKey\":{\"number\":1},\"value\":null}"),
        Arguments.of("/list/piped-insert-bulk",
            "{\"key\":\"k\",\"index\":0,\"values\":[null]}"),
        Arguments.of("/set/piped-insert-bulk", "{\"key\":\"k\",\"values\":[null]}"),
        Arguments.of("/map/piped-insert-bulk", "{\"key\":\"k\",\"elements\":{\"m\":null}}"),
        Arguments.of("/btree/piped-insert-bulk",
            "{\"key\":\"k\",\"elements\":[{\"bKey\":{\"number\":1},\"value\":null}]}"));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "SetRequest", "ListInsertRequest", "SetInsertRequest", "MapInsertRequest",
      "BTreeStoreRequest", "GetResponse", "GetsResponse", "BTreeElementResponse",
      "SetExistenceResponse"
  })
  @DisplayName("OpenAPI에서 value를 지원하는 JSON 타입으로 표현한다")
  void documentsJsonValueTypes(String schema) throws Exception {
    // given: value를 가진 요청과 응답 스키마를 제공한다.

    // expected
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.components.schemas." + schema + ".properties.value.type")
            .value(containsInAnyOrder("string", "number", "boolean", "object", "array")));
  }
}
