package com.jam2in.arcus.http.common;

import static org.mockito.Mockito.when;
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
import net.spy.memcached.ops.CollectionOperationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CollectionBulkHttpTest extends ClientHttpTest {

  @Test
  @DisplayName("모든 컬렉션의 bulk 삽입에서 키별 실패를 보존한다")
  void preservesPerKeyFailures() throws Exception {
    // given
    var failures = Map.of("b", new CollectionOperationStatus(false, "NOT_FOUND", CollectionResponse.NOT_FOUND));
    var future = CompletableFuture.completedFuture(failures);
    var keys = List.of("a", "b");
    when(client.asyncLopInsertBulk(keys, 0, "v", null)).thenReturn(future);
    when(client.asyncSopInsertBulk(keys, "v", null)).thenReturn(future);
    when(client.asyncMopInsertBulk(keys, "m", "v", null)).thenReturn(future);
    when(client.asyncBopInsertBulk(keys, 1L, null, "v", null)).thenReturn(future);
    var requests = Map.of(
        "/list/insert-bulk", "{\"keys\":[\"a\",\"b\"],\"index\":0,\"value\":\"v\"}",
        "/set/insert-bulk", "{\"keys\":[\"a\",\"b\"],\"value\":\"v\"}",
        "/map/insert-bulk", "{\"keys\":[\"a\",\"b\"],\"mKey\":\"m\",\"value\":\"v\"}",
        "/btree/insert-bulk", "{\"keys\":[\"a\",\"b\"],\"bKey\":{\"number\":1},\"value\":\"v\"}");

    // expected
    for (var request : requests.entrySet()) {
      mvc.perform(post(request.getKey())
              .contentType(MediaType.APPLICATION_JSON)
              .content(request.getValue()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.complete").value(true))
          .andExpect(jsonPath("$.content.failures.b").value("NOT_FOUND"))
          .andExpect(jsonPath("$.content.failures.a").doesNotExist());
    }
  }
}
