package com.jam2in.arcus.http.map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.List;
import java.util.Map;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class MapHttpTest extends ClientHttpTest {

  @Test
  @DisplayName("Map 생성과 삽입·교체·수정을 서로 다른 연산으로 연결한다")
  void createsStoresAndUpdates() throws Exception {
    // given
    var created = collection(true, CollectionResponse.CREATED);
    when(client.asyncMopCreate(eq("k"), eq(ElementValueType.OTHERS), any())).thenReturn(created);
    var stored = collection(true, CollectionResponse.STORED);
    when(client.asyncMopInsert("k", "m", "v", null)).thenReturn(stored);
    when(client.asyncMopUpsert("k", "m", "v", null)).thenReturn(stored);
    when(client.asyncMopUpdate("k", "m", "v")).thenReturn(stored);
    String body = "{\"key\":\"k\",\"mKey\":\"m\",\"value\":\"v\"}";

    // expected
    mvc.perform(post("/map/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/map/insert")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk());
    mvc.perform(patch("/map/upsert")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk());
    mvc.perform(patch("/map/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Map 전체·단일·다수 mKey 조회를 선택한다")
  void selectsMapGetOverloads() throws Exception {
    // given
    var found = collection(Map.<String,Object>of("m", "v"), CollectionResponse.END);
    when(client.asyncMopGet("k", false, false)).thenReturn(found);
    when(client.asyncMopGet("k", "m", false, false)).thenReturn(found);
    when(client.asyncMopGet("k", List.of("m"), false, false)).thenReturn(found);
    var requests = List.of(
        "{\"key\":\"k\"}",
        "{\"key\":\"k\",\"mKey\":\"m\"}",
        "{\"key\":\"k\",\"mKeys\":[\"m\"]}"
    );

    // expected
    for (String body : requests) {
      mvc.perform(post("/map/get")
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.value.m").value("v"));
    }
    verify(client).asyncMopGet("k", false, false);
    verify(client).asyncMopGet("k", "m", false, false);
    verify(client).asyncMopGet("k", List.of("m"), false, false);
  }

  @Test
  @DisplayName("클라이언트의 mKey 오류와 HTTP 선택 충돌을 구분한다")
  void validatesMapKeysAndSelectors() throws Exception {
    // given
    String oversizedKey = "가".repeat(84);
    when(client.asyncMopGet("k", oversizedKey, false, false))
        .thenThrow(new IllegalArgumentException("mKey is too long"));
    var oversizedKeyRequest = post("/map/get")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"mKey\":\""+"가".repeat(84)+"\"}");
    var conflictingSelectorsRequest = post("/map/get")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"mKey\":\"m\",\"mKeys\":[\"m\"]}");

    // expected
    mvc.perform(oversizedKeyRequest)
        .andExpect(status().isBadRequest());
    mvc.perform(conflictingSelectorsRequest)
        .andExpect(status().isBadRequest());
    verify(client).asyncMopGet("k", oversizedKey, false, false);
  }

  @Test
  @DisplayName("전체 Map 요소 삭제와 drop 옵션을 전달한다")
  void deletesAllElements() throws Exception {
    // given
    var deleted = collection(true, CollectionResponse.DELETED_DROPPED);
    when(client.asyncMopDelete("k", true)).thenReturn(deleted);

    // expected
    mvc.perform(delete("/map/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"dropIfEmpty\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.status").value("DELETED_DROPPED"));
  }
}
