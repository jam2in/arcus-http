package com.jam2in.arcus.http.set;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.Set;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SetHttpTest extends ClientHttpTest {

  @Test
  @DisplayName("Set 생성과 요소 삽입을 연결한다")
  void createsAndInserts() throws Exception {
    // given
    var created = collection(true, CollectionResponse.CREATED);
    when(client.asyncSopCreate(eq("k"), eq(ElementValueType.OTHERS), any())).thenReturn(created);
    var stored = collection(true, CollectionResponse.STORED);
    when(client.asyncSopInsert("k", "v", null)).thenReturn(stored);

    // expected
    mvc.perform(post("/set/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\"}"))
        .andExpect(status().isOk());

    mvc.perform(post("/set/insert")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"value\":\"v\"}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("요소 없음은 false이고 Set 없음은 404다")
  void distinguishesAbsentElementAndItem() throws Exception {
    // given
    var absent = collection(false, CollectionResponse.NOT_EXIST);
    var missing = collection(false, CollectionResponse.NOT_FOUND);
    when(client.asyncSopExist("k", "v")).thenReturn(absent);
    when(client.asyncSopExist("missing", "v")).thenReturn(missing);

    // expected
    mvc.perform(post("/set/exist")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"value\":\"v\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value").value(false));
    mvc.perform(post("/set/exist")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"missing\",\"value\":\"v\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("중복 요소는 충돌로 구분한다")
  void rejectsDuplicateElement() throws Exception {
    // given
    var result = collection(false, CollectionResponse.ELEMENT_EXISTS);
    when(client.asyncSopInsert("k", "v", null)).thenReturn(result);

    // expected
    mvc.perform(post("/set/insert")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"value\":\"v\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("Set 조회 후 삭제와 단일 요소 삭제를 연결한다")
  void readsAndDeletes() throws Exception {
    // given
    var found = collection(Set.<Object>of("v"), CollectionResponse.DELETED);
    when(client.asyncSopGet("k", 1, true, false)).thenReturn(found);
    var deleted = collection(true, CollectionResponse.DELETED);
    when(client.asyncSopDelete("k", "v", false)).thenReturn(deleted);

    // expected
    mvc.perform(patch("/set/get-and-delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"count\":1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value[0]").value("v"));
    mvc.perform(delete("/set/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"value\":\"v\"}"))
        .andExpect(status().isOk());
  }
}
