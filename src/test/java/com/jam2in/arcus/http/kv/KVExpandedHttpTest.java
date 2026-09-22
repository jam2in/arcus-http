package com.jam2in.arcus.http.kv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.CASValue;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.ops.StoreType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class KVExpandedHttpTest {

  @Autowired
  MockMvc mvc;
  @MockitoBean
  ArcusClientPool client;

  @SuppressWarnings("unchecked")
  private <T> OperationFuture<T> operation(T value, StatusCode code) throws Exception {
    OperationFuture<T> future = mock(OperationFuture.class);
    when(future.get()).thenReturn(value);
    when(future.getStatus()).thenReturn(new OperationStatus(code == StatusCode.SUCCESS, code.name(), code));
    return future;
  }

  @ParameterizedTest
  @CsvSource({"add,POST", "replace,PATCH", "append,PATCH", "prepend,PATCH"})
  @DisplayName("저장 변형을 올바른 HTTP 메서드와 클라이언트 연산으로 연결한다")
  void storesThroughDocumentedMethods(String name, String method) throws Exception {
    // given
    var future = operation(true, StatusCode.SUCCESS);
    String body = "{\"key\":\"k\",\"value\":\"v\"}";
    switch (name) {
      case "add" -> {
        when(client.add("k", 60, "v")).thenReturn(future);
        body = "{\"key\":\"k\",\"value\":\"v\",\"expireTime\":60}";
      }
      case "replace" -> {
        when(client.replace("k", 60, "v")).thenReturn(future);
        body = "{\"key\":\"k\",\"value\":\"v\",\"expireTime\":60}";
      }
      case "append" -> when(client.append(0, "k", "v")).thenReturn(future);
      case "prepend" -> when(client.prepend(0, "k", "v")).thenReturn(future);
    }

    // expected
    mvc.perform(request(HttpMethod.valueOf(method), "/kv/" + name)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.stored").value(true));
    verify(future).get();
    assertThat(mockingDetails(client).getInvocations()).hasSize(1);
  }

  @Test
  @DisplayName("add의 기존 키 충돌과 replace의 키 미존재를 구분한다")
  void distinguishesConditionalStoreFailures() throws Exception {
    // given
    var rejected = operation(false, StatusCode.ERR_NOT_STORED);
    when(client.add("k", 0, "v")).thenReturn(rejected);
    when(client.replace("k", 0, "v")).thenReturn(rejected);
    String body = "{\"key\":\"k\",\"value\":\"v\",\"expireTime\":0}";

    // expected
    mvc.perform(post("/kv/add")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isConflict());
    mvc.perform(patch("/kv/replace")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("bulk 조회는 미존재 키를 빈 문자열과 구분한다")
  void bulkGetPreservesMissingKeys() throws Exception {
    // given
    BulkFuture<Map<String, Object>> future = mock(BulkFuture.class);
    when(future.get()).thenReturn(Map.of("empty", ""));
    when(client.asyncGetBulk(List.of("empty", "missing"))).thenReturn(future);

    // expected
    mvc.perform(post("/kv/get-bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"keys\":[\"empty\",\"missing\"]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.values.empty").value(""))
        .andExpect(jsonPath("$.content.missingKeys[0]").value("missing"));
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("CAS 값은 클라이언트의 long 값으로 반환한다")
  void getsPreservesCasPrecision() throws Exception {
    // given
    GetFuture<CASValue<Object>> future = mock(GetFuture.class);
    when(future.get()).thenReturn(new CASValue<>(-1L, "v"));
    when(client.asyncGets("k")).thenReturn(future);

    // expected
    mvc.perform(post("/kv/gets")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.cas").value(-1));
  }

  @Test
  @SuppressWarnings("unchecked")
  @DisplayName("bulk 저장은 부분 실패를 성공으로 숨기지 않는다")
  void bulkStorePreservesPerKeyFailure() throws Exception {
    // given
    Future<Map<String, OperationStatus>> future = mock(Future.class);
    when(future.get()).thenReturn(Map.of("k", new OperationStatus(false, "private detail", StatusCode.ERR_NOT_STORED)));
    when(client.asyncStoreBulk(StoreType.add, List.of("k"), 0, "v")).thenReturn(future);

    // expected
    mvc.perform(post("/kv/store-bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"ADD\",\"keys\":[\"k\"],\"value\":\"v\",\"expireTime\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.complete").value(true))
        .andExpect(jsonPath("$.content.failures.k").value("ERR_NOT_STORED"));
  }

  @Test
  @DisplayName("증가 연산의 초기값과 TTL 오버로드를 전달한다")
  void incrementsWithInitialValue() throws Exception {
    // given
    var future = operation(7L, StatusCode.SUCCESS);
    when(client.asyncIncr("k", 2, 7L, 60)).thenReturn(future);

    // expected
    mvc.perform(patch("/kv/incr")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"by\":2,\"initial\":7,\"expireTime\":60}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value").value("7"));
    verify(client).asyncIncr("k", 2, 7L, 60);
  }

  @Test
  @DisplayName("초기값과 TTL 중 하나만 지정하면 클라이언트를 호출하지 않는다")
  void rejectsIncompleteCounterDefaults() throws Exception {
    // given
    var request = patch("/kv/decr")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"by\":2,\"initial\":7}");

    // expected
    mvc.perform(request)
        .andExpect(status().isBadRequest());
    verifyNoInteractions(client);
  }

  @Test
  @DisplayName("삭제와 TTL 갱신에 DELETE와 PATCH를 사용한다")
  void deletesAndTouchesItems() throws Exception {
    // given
    var future = operation(true, StatusCode.SUCCESS);
    when(client.delete("k")).thenReturn(future);
    when(client.touch("k", 60)).thenReturn(future);

    // expected
    mvc.perform(delete("/kv/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\"}"))
        .andExpect(status().isOk());
    mvc.perform(patch("/kv/touch")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"expireTime\":60}"))
        .andExpect(status().isOk());
    mvc.perform(post("/kv/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\"}"))
        .andExpect(status().isMethodNotAllowed());
  }
}
