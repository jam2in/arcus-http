package com.jam2in.arcus.http.list;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.List;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ListHttpTest extends ClientHttpTest {

  @Test
  @DisplayName("문자열 타입으로 빈 List를 생성한다")
  void createsStringList() throws Exception {
    // given
    var future = collection(true, CollectionResponse.CREATED);
    when(client.asyncLopCreate(eq("k"), eq(ElementValueType.OTHERS), any())).thenReturn(future);

    // expected
    mvc.perform(post("/list/create")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.status").value("CREATED"));
  }

  @Test
  @DisplayName("음수 위치 삽입과 범위 조회를 전달한다")
  void insertsAndReadsNegativeIndices() throws Exception {
    // given
    var stored = collection(true, CollectionResponse.STORED);
    when(client.asyncLopInsert("k", -1, "v", null)).thenReturn(stored);
    var found = collection(List.<Object>of("a", "b"), CollectionResponse.END);
    when(client.asyncLopGet("k", 0, -1, false, false)).thenReturn(found);

    // expected
    mvc.perform(post("/list/insert")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"index\":-1,\"value\":\"v\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/list/get")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"from\":0,\"to\":-1}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value[1]").value("b"));
  }

  @Test
  @DisplayName("빈 조회 결과와 키 미존재를 구분한다")
  void distinguishesEmptyCollectionAndMissingItem() throws Exception {
    // given
    var empty = collection(List.<Object>of(), CollectionResponse.NOT_FOUND_ELEMENT);
    var missing = collection((List<Object>) null, CollectionResponse.NOT_FOUND);
    when(client.asyncLopGet("empty", 0, false, false)).thenReturn(empty);
    when(client.asyncLopGet("missing", 0, false, false)).thenReturn(missing);

    // expected
    mvc.perform(post("/list/get")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"empty\",\"from\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value").isEmpty());
    mvc.perform(post("/list/get")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"missing\",\"from\":0}"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("조회 후 삭제는 PATCH로 수행하고 dropped 상태를 보존한다")
  void readsAndDeletesAtomically() throws Exception {
    // given
    var result = collection(List.<Object>of("v"), CollectionResponse.DELETED_DROPPED);
    when(client.asyncLopGet("k", 0, true, true)).thenReturn(result);

    // expected
    mvc.perform(patch("/list/get-and-delete")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"from\":0,\"dropIfEmpty\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.status").value("DELETED_DROPPED"));
    verify(client, times(1)).asyncLopGet("k", 0, true, true);
  }

  @Test
  @DisplayName("중복 bulk 키와 조회에서의 drop 옵션을 거부한다")
  void rejectsInvalidBulkAndReadOptions() throws Exception {
    // given
    var duplicateKeysRequest = post("/list/insert-bulk")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"keys\":[\"k\",\"k\"],\"index\":0,\"value\":\"v\"}");
    var dropOnReadRequest = post("/list/get")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"from\":0,\"dropIfEmpty\":true}");

    // expected
    mvc.perform(duplicateKeysRequest)
        .andExpect(status().isBadRequest());
    mvc.perform(dropOnReadRequest)
        .andExpect(status().isBadRequest());
    verifyNoInteractions(client);
  }

  @Test
  @DisplayName("List 요소를 DELETE로 삭제한다")
  void deletesRange() throws Exception {
    // given
    var deleted = collection(true, CollectionResponse.DELETED);
    when(client.asyncLopDelete("k", 0, 1, false)).thenReturn(deleted);

    // expected
    mvc.perform(delete("/list/delete")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"from\":0,\"to\":1}"))
        .andExpect(status().isOk());
  }
}
