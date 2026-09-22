package com.jam2in.arcus.http.common;

import static com.jam2in.arcus.http.support.TestClientFutures.getSuccess;
import static com.jam2in.arcus.http.support.TestClientFutures.storeSuccess;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StatusCode;
import net.spy.memcached.ops.StoreType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

class ExpirationHttpTest extends ClientHttpTest {

  @ParameterizedTest
  @ValueSource(ints = {-1, 2592001, Integer.MAX_VALUE})
  @SuppressWarnings("unchecked")
  @DisplayName("KV bulk 저장과 증감 및 TTL 갱신은 만료 시간을 그대로 전달한다")
  void preservesExpirationAcrossKvOperations(int expireTime) throws Exception {
    // given
    var stored = storeSuccess(true);
    var found = getSuccess("v");
    when(client.asyncStoreBulk(StoreType.set, List.of("k"), expireTime, "v"))
        .thenReturn(CompletableFuture.completedFuture(Map.<String, OperationStatus>of()));
    OperationFuture<Long> counter = mock(OperationFuture.class);
    when(counter.get()).thenReturn(7L);
    when(counter.getStatus()).thenReturn(new OperationStatus(true, "SUCCESS", StatusCode.SUCCESS));
    when(client.asyncIncr("k", 1, 7L, expireTime)).thenReturn(counter);
    when(client.touch("k", expireTime)).thenReturn(stored);
    when(client.asyncGetAndTouch("k", expireTime)).thenReturn(found);

    // expected
    mvc.perform(post("/kv/store-bulk").contentType(MediaType.APPLICATION_JSON)
            .content("{\"mode\":\"SET\",\"keys\":[\"k\"],\"value\":\"v\",\"expireTime\":" + expireTime + "}"))
        .andExpect(status().isOk());
    mvc.perform(patch("/kv/incr").contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"by\":1,\"initial\":7,\"expireTime\":" + expireTime + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value").value("7"));
    for (String operation : List.of("touch", "get-and-touch")) {
      mvc.perform(patch("/kv/" + operation).contentType(MediaType.APPLICATION_JSON)
              .content("{\"key\":\"k\",\"expireTime\":" + expireTime + "}"))
          .andExpect(status().isOk());
    }
    verify(client).asyncStoreBulk(StoreType.set, List.of("k"), expireTime, "v");
    verify(client).asyncIncr("k", 1, 7L, expireTime);
    verify(client).touch("k", expireTime);
    verify(client).asyncGetAndTouch("k", expireTime);
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 2592001, Integer.MAX_VALUE})
  @DisplayName("컬렉션 생성과 자동 생성 및 속성 변경은 만료 시간을 그대로 전달한다")
  void preservesExpirationInAttributes(int expireTime) throws Exception {
    // given
    var created = collection(true, CollectionResponse.CREATED);
    var inserted = collection(true, CollectionResponse.CREATED_STORED);
    var updated = collection(true, CollectionResponse.OK);
    when(client.asyncLopCreate(eq("k"), eq(ElementValueType.OTHERS),
        argThat(attributes -> attributes.getExpireTime() == expireTime))).thenReturn(created);
    when(client.asyncLopInsert(eq("k"), eq(0), eq("v"),
        argThat(attributes -> attributes.getExpireTime() == expireTime))).thenReturn(inserted);
    when(client.asyncSetAttr(eq("k"),
        argThat(attributes -> attributes.getExpireTime() == expireTime))).thenReturn(updated);

    // expected
    mvc.perform(post("/list/create").contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"attributes\":{\"expireTime\":" + expireTime + "}}"))
        .andExpect(status().isOk());
    mvc.perform(post("/list/insert").contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"index\":0,\"value\":\"v\",\"attributesForCreate\":{\"expireTime\":"
                + expireTime + "}}"))
        .andExpect(status().isOk());
    mvc.perform(patch("/item/set-attr").contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"attributes\":{\"expireTime\":" + expireTime + "}}"))
        .andExpect(status().isOk());
    verify(client).asyncLopCreate(eq("k"), eq(ElementValueType.OTHERS),
        argThat(attributes -> attributes.getExpireTime() == expireTime));
    verify(client).asyncLopInsert(eq("k"), eq(0), eq("v"),
        argThat(attributes -> attributes.getExpireTime() == expireTime));
    verify(client).asyncSetAttr(eq("k"), argThat(attributes -> attributes.getExpireTime() == expireTime));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "POST | /kv/set | {\"key\":\"k\",\"value\":\"v\",\"expireTime\":-2}",
      "POST | /kv/store-bulk | {\"mode\":\"SET\",\"keys\":[\"k\"],\"value\":\"v\",\"expireTime\":-2}",
      "PATCH | /kv/incr | {\"key\":\"k\",\"by\":1,\"initial\":7,\"expireTime\":-2}",
      "PATCH | /kv/touch | {\"key\":\"k\",\"expireTime\":-2}",
      "POST | /list/create | {\"key\":\"k\",\"attributes\":{\"expireTime\":-2}}",
      "PATCH | /item/set-attr | {\"key\":\"k\",\"attributes\":{\"expireTime\":-2}}"
  })
  @DisplayName("지원하지 않는 음수 만료 시간은 Arcus 호출 전에 거부한다")
  void rejectsInvalidNegativeExpiration(String method, String path, String body) throws Exception {
    mvc.perform(request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(client);
  }

  @ParameterizedTest
  @ValueSource(strings = {"SetRequest", "StoreBulkRequest", "CounterRequest", "TouchRequest",
      "CreateAttributes", "UpdateAttributes"})
  @DisplayName("OpenAPI의 모든 만료 시간 필드에 특수값과 Unix timestamp를 안내한다")
  void documentsExpirationWithoutThirtyDayMaximum(String schema) throws Exception {
    String path = "$.components.schemas." + schema + ".properties.expireTime";
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath(path + ".minimum").value(-1))
        .andExpect(jsonPath(path + ".maximum").doesNotExist())
        .andExpect(jsonPath(path + ".description").value(org.hamcrest.Matchers.containsString("Unix timestamp")));
  }
}
