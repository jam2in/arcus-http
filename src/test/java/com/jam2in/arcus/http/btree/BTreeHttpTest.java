package com.jam2in.arcus.http.btree;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.List;
import java.util.Map;
import net.spy.memcached.collection.BTreeElement;
import net.spy.memcached.collection.BTreeGetResult;
import net.spy.memcached.collection.BTreeOrder;
import net.spy.memcached.collection.ByteArrayBKey;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.Element;
import net.spy.memcached.collection.ElementFlagUpdate;
import net.spy.memcached.collection.ElementMultiFlagsFilter;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.collection.SMGetElement;
import net.spy.memcached.collection.SMGetTrimKey;
import net.spy.memcached.internal.BTreeStoreAndGetFuture;
import net.spy.memcached.internal.CollectionGetBulkFuture;
import net.spy.memcached.internal.SMGetFuture;
import net.spy.memcached.ops.CollectionOperationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.springframework.http.MediaType;

class BTreeHttpTest extends ClientHttpTest {

  @Test
  @DisplayName("숫자·바이트 bKey 삽입과 PATCH upsert를 연결한다")
  void storesBothKeyTypes() throws Exception {
    // given
    var created = collection(true, CollectionResponse.CREATED);
    when(client.asyncBopCreate(eq("k"), eq(ElementValueType.OTHERS), any())).thenReturn(created);
    var stored = collection(true, CollectionResponse.STORED);
    when(client.asyncBopInsert(eq("k"), eq(1L), isNull(), eq("v"), isNull())).thenReturn(stored);
    when(client.asyncBopUpsert(eq("k"), aryEq(new byte[] {1}), aryEq(new byte[] {2}), eq("v"),
        isNull())).thenReturn(stored);

    // expected
    mvc.perform(
            post("/btree/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"k\"}"))
        .andExpect(status().isOk());
    mvc.perform(post("/btree/insert")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"bKey\":{\"number\":1},\"value\":\"v\"}"))
        .andExpect(status().isOk());
    mvc.perform(patch("/btree/upsert")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"bKey\":{\"hex\":\"01\"},\"eFlag\":\"02\",\"value\":\"v\"}"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("범위 조회는 순서·trim 상태·EFlag를 보존한다")
  void readsRangeAndPreservesMetadata() throws Exception {
    // given
    var result =
        collection(Map.<Long, Element<Object>>of(1L, new Element<>(1L, "v", new byte[] {2})),
            CollectionResponse.TRIMMED);
    when(client.asyncBopGet(eq("k"), eq(0L), eq(9L), any(), eq(0), eq(10), eq(false),
        eq(false))).thenReturn(result);

    // expected
    mvc.perform(post("/btree/get")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
            {"key":"k","from":{"number":0},"to":{"number":9},"count":10,
             "filter":{"compare":"Equal","values":["02","03"]}}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.status").value("TRIMMED"))
        .andExpect(jsonPath("$.content.value[0].bKey.number").value(1))
        .andExpect(jsonPath("$.content.value[0].eFlag").value("02"));
    verify(client).asyncBopGet(eq("k"), eq(0L), eq(9L),
        argThat(f -> f instanceof ElementMultiFlagsFilter), eq(0), eq(10), eq(false), eq(false));
  }

  @Test
  @DisplayName("바이트 bKey 조회 후 삭제는 PATCH로 한 번만 실행한다")
  void readsAndDeletesByteKey() throws Exception {
    // given
    var result = collection(Map.of(new ByteArrayBKey(new byte[] {1}),
            new Element<Object>(new byte[] {1}, "v", (byte[]) null)),
        CollectionResponse.DELETED_DROPPED);
    when(client.asyncBopGet(eq("k"), aryEq(new byte[] {1}), isNull(), eq(true),
        eq(true))).thenReturn(result);

    // expected
    mvc.perform(patch("/btree/get-and-delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"from\":{\"hex\":\"01\"},\"dropIfEmpty\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value[0].bKey.hex").value("01"));
    verify(client, times(1)).asyncBopGet(eq("k"), aryEq(new byte[] {1}), isNull(), eq(true),
        eq(true));
  }

  @Test
  @DisplayName("잘린 요소를 저장 결과와 함께 반환한다")
  @SuppressWarnings("unchecked")
  void returnsTrimmedElement() throws Exception {
    // given
    BTreeStoreAndGetFuture<Boolean, Object> future = mock(BTreeStoreAndGetFuture.class);
    when(future.get()).thenReturn(true);
    when(future.getOperationStatus()).thenReturn(
        new CollectionOperationStatus(true, "STORED", CollectionResponse.STORED));
    when(future.getElement()).thenReturn(new Element<>(1L, "old", (byte[]) null));
    when(client.asyncBopInsertAndGetTrimmed("k", 2L, null, "new", null)).thenReturn(future);

    // expected
    mvc.perform(post("/btree/insert-and-get-trimmed")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"bKey\":{\"number\":2},\"value\":\"new\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value.trimmed.value").value("old"));
  }

  @Test
  @DisplayName("EFlag 제거 및 초기값을 사용하는 증감을 지원한다")
  void updatesFlagsAndCounters() throws Exception {
    // given
    var updated = collection(true, CollectionResponse.UPDATED);
    when(client.asyncBopUpdate("k", 1L, ElementFlagUpdate.RESET_FLAG, null)).thenReturn(updated);
    var count = collection(-1L, CollectionResponse.OK);
    when(client.asyncBopIncr("k", 1L, 2, 10L, null)).thenReturn(count);

    // expected
    mvc.perform(patch("/btree/update")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"bKey\":{\"number\":1},\"eFlagUpdate\":{\"reset\":true}}"))
        .andExpect(status().isOk());
    mvc.perform(patch("/btree/incr")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"bKey\":{\"number\":1},\"by\":2,\"initial\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value").value("18446744073709551615"));
  }

  @Test
  @DisplayName("위치 조회는 요소의 실제 bKey 타입을 유지한다")
  void returnsPositions() throws Exception {
    // given
    var result = collection(Map.of(3, new Element<Object>(new byte[] {1}, "v", (byte[]) null)),
        CollectionResponse.END);
    when(client.asyncBopGetByPosition("k", BTreeOrder.DESC, 3)).thenReturn(result);

    // expected
    mvc.perform(post("/btree/get-by-position")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"order\":\"DESC\",\"from\":3}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value.3.bKey.hex").value("01"));
  }

  @Test
  @DisplayName("smget의 누락 키와 잘린 키를 숨기지 않는다")
  @SuppressWarnings("unchecked")
  void preservesSmgetPartialResults() throws Exception {
    // given
    SMGetFuture<List<SMGetElement<Object>>> future = mock(SMGetFuture.class);
    when(future.get()).thenReturn(List.of(new SMGetElement<>("a", 1L, null, "v")));
    when(future.getOperationStatus()).thenReturn(
        new CollectionOperationStatus(true, "TRIMMED", CollectionResponse.TRIMMED));
    when(future.getMissedKeys()).thenReturn(Map.of("b",
        new CollectionOperationStatus(false, "NOT_FOUND", CollectionResponse.NOT_FOUND)));
    when(future.getTrimmedKeys()).thenReturn(List.of(new SMGetTrimKey("a", 2L)));
    when(client.asyncBopSortMergeGet(List.of("a", "b"), 0L, 9L, null, 10, false)).thenReturn(
        future);

    // expected
    mvc.perform(post("/btree/smget")
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"keys\":[\"a\",\"b\"],\"from\":{\"number\":0},\"to\":{\"number\":9},\"count\":10}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value.missedKeys.b").value("NOT_FOUND"))
        .andExpect(jsonPath("$.content.value.trimmedKeys[0].bKey.number").value(2));
  }

  @Test
  @DisplayName("잘못된 bKey·필터·조회 옵션을 전송 전에 거부한다")
  void rejectsInvalidParameters() throws Exception {
    // given
    var requests = List.of(
        "{\"key\":\"k\",\"from\":{\"number\":1,\"hex\":\"01\"}}",
        "{\"key\":\"k\",\"from\":{\"hex\":\"gg\"}}",
        "{\"key\":\"k\",\"from\":{\"number\":1},\"to\":{\"hex\":\"01\"}}",
        "{\"key\":\"k\",\"from\":{\"number\":1},\"dropIfEmpty\":true}",
        "{\"key\":\"k\",\"from\":{\"number\":1},\"filter\":{\"compare\":\"Equal\",\"values\":[\"01\",\"0102\"]}}");

    // expected
    for (String json : requests) {
      mvc.perform(post("/btree/get")
              .contentType(MediaType.APPLICATION_JSON)
              .content(json))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(client);
  }

  @Test
  @DisplayName("bulk 조회는 키별 오류와 정상 요소를 함께 반환한다")
  @SuppressWarnings("unchecked")
  void preservesBulkGetFailures() throws Exception {
    // given
    CollectionGetBulkFuture<Map<String, BTreeGetResult<Long, Object>>> future =
        mock(CollectionGetBulkFuture.class);
    BTreeGetResult<Long, Object> good = mock(BTreeGetResult.class);
    BTreeGetResult<Long, Object> missing = mock(BTreeGetResult.class);
    when(good.getElements()).thenReturn(Map.of(1L, new BTreeElement<>(1L, null, "v")));
    when(good.getCollectionResponse()).thenReturn(
        new CollectionOperationStatus(true, "OK", CollectionResponse.OK));
    when(missing.getElements()).thenReturn(null);
    when(missing.getCollectionResponse()).thenReturn(
        new CollectionOperationStatus(false, "NOT_FOUND", CollectionResponse.NOT_FOUND));
    when(future.get()).thenReturn(Map.of("a", good, "b", missing));
    when(client.asyncBopGetBulk(List.of("a", "b"), 0L, 9L, null, 0, 10)).thenReturn(future);

    // expected
    mvc.perform(post("/btree/get-bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
            {"keys":["a","b"],"from":{"number":0},"to":{"number":9},"count":10}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.a.value[0].value").value("v"))
        .andExpect(jsonPath("$.content.b.status").value("NOT_FOUND"));
  }

  @Test
  @DisplayName("개수·위치·주변 조회와 범위 삭제를 연결한다")
  void countsFindsAndDeletes() throws Exception {
    // given
    var count = collection(2, CollectionResponse.OK);
    var elements =
        collection(Map.of(2, new Element<Object>(1L, "v", (byte[]) null)), CollectionResponse.END);
    var deleted = collection(true, CollectionResponse.DELETED);
    when(client.asyncBopGetItemCount("k", 0L, 9L, null)).thenReturn(count);
    when(client.asyncBopFindPosition("k", 1L, BTreeOrder.ASC)).thenReturn(count);
    when(client.asyncBopFindPositionWithGet("k", 1L, BTreeOrder.ASC, 0)).thenReturn(elements);
    when(client.asyncBopDelete("k", 0L, 9L, null, 0, false)).thenReturn(deleted);

    // expected
    mvc.perform(post("/btree/get-item-count")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
            {"key":"k","from":{"number":0},"to":{"number":9}}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value").value(2));
    mvc.perform(post("/btree/find-position")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
            {"key":"k","bKey":{"number":1},"order":"ASC"}
            """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value").value(2));
    mvc.perform(
            post("/btree/find-position-with-get")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                {"key":"k","bKey":{"number":1},"order":"ASC","count":0}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value.2.value").value("v"));
    mvc.perform(delete("/btree/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
            {"key":"k","from":{"number":0},"to":{"number":9}}
            """))
        .andExpect(status().isOk());
  }

  private static byte[] aryEq(byte[] bytes) {
    return AdditionalMatchers.aryEq(bytes);
  }
}
