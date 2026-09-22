package com.jam2in.arcus.http.common;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementFlagUpdate;
import net.spy.memcached.ops.CollectionOperationStatus;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PipedHttpTest extends ClientHttpTest {

  @Test
  @DisplayName("piped 삽입은 실패 위치와 불완전한 처리 상태를 보존한다")
  void preservesPartialBatchFailures() throws Exception {
    // given
    var failures = Map.of(1, new CollectionOperationStatus(false, "OVERFLOWED", CollectionResponse.OVERFLOWED));
    var future = collection(failures, CollectionResponse.FAILED_END);
    when(client.asyncLopPipedInsertBulk(eq("k"), eq(0), eq(List.of("a", "b")), isNull())).thenReturn(future);
    when(client.asyncSopPipedInsertBulk(eq("k"), eq(List.of("a", "b")), isNull())).thenReturn(future);
    when(client.asyncMopPipedInsertBulk(eq("k"), eq(Map.of("a", "v")), isNull())).thenReturn(future);
    when(client.asyncBopPipedInsertBulk(eq("k"), anyList(), isNull())).thenReturn(future);
    var requests = Map.of(
        "/list/piped-insert-bulk", "{\"key\":\"k\",\"index\":0,\"values\":[\"a\",\"b\"]}",
        "/set/piped-insert-bulk", "{\"key\":\"k\",\"values\":[\"a\",\"b\"]}",
        "/map/piped-insert-bulk", "{\"key\":\"k\",\"elements\":{\"a\":\"v\"}}",
        "/btree/piped-insert-bulk", "{\"key\":\"k\",\"elements\":[{\"bKey\":{\"hex\":\"01\"},\"value\":\"v\"}]}");

    // expected
    for (var request : requests.entrySet()) {
      mvc.perform(post(request.getKey())
              .contentType(MediaType.APPLICATION_JSON)
              .content(request.getValue()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.status").value("FAILED_END"))
          .andExpect(jsonPath("$.content.complete").value(false))
          .andExpect(jsonPath("$.content.failures.1").value("OVERFLOWED"));
    }
  }

  @Test
  @DisplayName("piped 수정과 Set 일괄 존재 검사를 연결한다")
  void updatesAndChecksExistence() throws Exception {
    // given
    var updated = collection(Map.<Integer, CollectionOperationStatus>of(), CollectionResponse.END);
    when(client.asyncMopPipedUpdateBulk("k", Map.of("m", "v"))).thenReturn(updated);
    when(client.asyncBopPipedUpdateBulk(eq("k"), anyList())).thenReturn(updated);
    var exists = collection(Map.<Object, Boolean>of("a", true, "b", false), CollectionResponse.END);
    when(client.asyncSopPipedExistBulk("k", List.of("a", "b"))).thenReturn(exists);

    // expected
    mvc.perform(patch("/map/piped-update-bulk")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"elements\":{\"m\":\"v\"}}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.complete").value(true));
    mvc.perform(patch("/btree/piped-update-bulk")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"elements\":[{\"bKey\":{\"number\":1},\"eFlagUpdate\":{\"reset\":true}}]}"))
        .andExpect(status().isOk());
    verify(client).asyncBopPipedUpdateBulk(eq("k"), argThat(elements -> elements.getFirst().getElementFlagUpdate() == ElementFlagUpdate.RESET_FLAG));
    mvc.perform(post("/set/piped-exist-bulk")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"values\":[\"a\",\"b\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value[0].exists").value(true))
        .andExpect(jsonPath("$.content.value[1].exists").value(false));
  }

  @Test
  @DisplayName("KV bulk 삭제는 키별 실패를 반환한다")
  void deletesMultipleKeys() throws Exception {
    // given
    when(client.asyncDeleteBulk(List.of("a", "b"))).thenReturn(CompletableFuture.completedFuture(
        Map.of("b", new OperationStatus(false, "NOT_FOUND"))));

    // expected
    mvc.perform(delete("/kv/delete-bulk")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"keys\":[\"a\",\"b\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.failures.b").value("ERR_NOT_FOUND"));
  }

  @Test
  @DisplayName("빈 piped 목록과 혼합 bKey 타입은 전송 전에 거부한다")
  void rejectsInvalidBatches() throws Exception {
    // given
    var emptyBatchRequest = post("/list/piped-insert-bulk")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"index\":0,\"values\":[]}");
    var mixedKeyTypesRequest = post("/btree/piped-insert-bulk")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"elements\":[{\"bKey\":{\"number\":1},\"value\":\"a\"},{\"bKey\":{\"hex\":\"02\"},\"value\":\"b\"}]}");

    // expected
    mvc.perform(emptyBatchRequest)
        .andExpect(status().isBadRequest());
    mvc.perform(mixedKeyTypesRequest)
        .andExpect(status().isBadRequest());
    verifyNoInteractions(client);
  }

  @Test
  @DisplayName("bulk 저장은 keys 없이 values 맵만으로도 요청할 수 있다")
  void storesPerKeyValuesWithoutKeysList() throws Exception {
    // given
    when(client.asyncStoreBulk(StoreType.set, Map.of("a", "v"), 60))
        .thenReturn(CompletableFuture.completedFuture(Map.of()));

    // expected
    mvc.perform(post("/kv/store-bulk")
            .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"mode":"SET","expireTime":60,"values":{"a":"v"}}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.complete").value(true));
  }
}
