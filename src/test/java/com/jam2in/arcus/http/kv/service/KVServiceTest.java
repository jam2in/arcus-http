package com.jam2in.arcus.http.kv.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.kv.adapter.KVAdapter;
import com.jam2in.arcus.http.kv.dto.request.GetBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.GetRequest;
import com.jam2in.arcus.http.kv.dto.request.TouchRequest;
import com.jam2in.arcus.http.kv.dto.response.GetResponse;
import com.jam2in.arcus.http.kv.dto.response.GetsResponse;
import java.util.List;
import java.util.Map;
import net.spy.memcached.CASValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class KVServiceTest {

  private final KVAdapter adapter = mock(KVAdapter.class);
  private final KVService service = new KVService(adapter);

  @Test
  @DisplayName("Service가 요청 순서대로 중복 없는 누락 키를 계산한다")
  void composesMissingKeysInRequestOrder() {
    // given
    var keys = List.of("missing-b", "found", "missing-a", "missing-b");
    when(adapter.getBulk(keys)).thenReturn(Map.of("found", "value"));

    // when
    var result = service.getBulk(new GetBulkRequest(keys));

    // then
    assertThat(result.values()).containsExactlyEntriesOf(Map.of("found", "value"));
    assertThat(result.missingKeys()).containsExactly("missing-b", "missing-a");
    verify(adapter).getBulk(keys);
    verifyNoMoreInteractions(adapter);
  }

  @Test
  @DisplayName("CAS 일괄 조회 응답에 Object 값과 long CAS과 누락 키를 담는다")
  void composesVersionedResponseWithObjectValue() {
    // given
    var keys = List.of("found", "missing");
    var value = new CASValue<Object>(Long.MIN_VALUE, Map.of("name", "hello"));
    when(adapter.getsBulk(keys)).thenReturn(Map.of("found", value));

    // when
    var result = service.getsBulk(new GetBulkRequest(keys));

    // then
    assertThat(result.values().get("found"))
        .isEqualTo(new GetsResponse("found", Map.of("name", "hello"), Long.MIN_VALUE));
    assertThat(result.missingKeys()).containsExactly("missing");
  }

  @Test
  @DisplayName("단일 조회의 미존재 판단은 Service가 수행한다")
  void rejectsMissingValue() {
    // given
    when(adapter.get("missing")).thenReturn(null);

    // expected
    assertThatThrownBy(() -> service.get("missing"))
        .isInstanceOfSatisfying(ArcusHttpException.class,
            exception -> assertThat(exception.code()).isEqualTo(ErrorCode.KEY_NOT_FOUND));
  }

  @Test
  @DisplayName("CAS 조회의 미존재 판단은 Service가 수행한다")
  void rejectsMissingVersionedValue() {
    // given
    when(adapter.gets("missing")).thenReturn(null);

    // expected
    assertThatThrownBy(() -> service.gets(new GetRequest("missing")))
        .isInstanceOfSatisfying(ArcusHttpException.class,
            exception -> assertThat(exception.code()).isEqualTo(ErrorCode.KEY_NOT_FOUND));
  }

  @Test
  @DisplayName("조회와 TTL 변경은 단일 Adapter 연산으로 처리한다")
  void getsAndTouchesWithoutSeparateLookup() {
    // given
    when(adapter.getAndTouch("key", 60)).thenReturn("value");

    // when
    var response = service.getAndTouch(new TouchRequest("key", 60));

    // then
    assertThat(response).isEqualTo(new GetResponse("key", "value"));
    verify(adapter).getAndTouch("key", 60);
    verifyNoMoreInteractions(adapter);
  }
}
