package com.jam2in.arcus.http.collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.stream.Stream;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

class CollectionCreateHttpTest extends ClientHttpTest {

  @ParameterizedTest
  @ValueSource(strings = {"list", "set", "map", "btree"})
  @DisplayName("네 컬렉션은 요청 타입 선택 없이 OTHERS로 생성한다")
  void createsSerializedCollection(String collectionType) throws Exception {
    // given
    var created = collection(true, CollectionResponse.CREATED);
    switch (collectionType) {
      case "list" -> when(client.asyncLopCreate(eq("typed"), eq(ElementValueType.OTHERS), any()))
          .thenReturn(created);
      case "set" -> when(client.asyncSopCreate(eq("typed"), eq(ElementValueType.OTHERS), any()))
          .thenReturn(created);
      case "map" -> when(client.asyncMopCreate(eq("typed"), eq(ElementValueType.OTHERS), any()))
          .thenReturn(created);
      case "btree" -> when(client.asyncBopCreate(eq("typed"), eq(ElementValueType.OTHERS), any()))
          .thenReturn(created);
      default -> throw new IllegalArgumentException(collectionType);
    }
    String body = "{\"key\":\"typed\"}";

    // expected
    mvc.perform(post("/" + collectionType + "/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.status").value("CREATED"));
  }

  @ParameterizedTest
  @MethodSource("invalidTypes")
  @DisplayName("삭제된 valueType 필드는 알 수 없는 요청 필드로 거부한다")
  void rejectsRemovedValueType(String collectionType, String valueTypeJson) throws Exception {
    // given
    String body = "{\"key\":\"typed\",\"valueType\":" + valueTypeJson + "}";

    // expected
    mvc.perform(post("/" + collectionType + "/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(client);
  }

  static Stream<Arguments> invalidTypes() {
    return Stream.of("list", "set", "map", "btree").flatMap(collectionType ->
        Stream.of("\"OBJECT\"", "\"STRING\"", "null")
            .map(valueType -> Arguments.of(collectionType, valueType)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"List", "Set", "Map", "BTree"})
  @DisplayName("OpenAPI에서 삭제한 valueType 선택지를 노출하지 않는다")
  void omitsValueTypeFromDocumentation(String collectionType) throws Exception {
    // given: 요청 DTO 이름을 파라미터로 제공한다.
    String schemaPath = "$.components.schemas." + collectionType
        + "CreateRequest.properties.valueType";

    // expected
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(schemaPath).doesNotExist())
        .andExpect(jsonPath("$.components.schemas.CollectionValueType").doesNotExist());
  }
}
