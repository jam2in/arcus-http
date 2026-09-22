package com.jam2in.arcus.http.kv.adapter;

import static com.jam2in.arcus.http.support.TestClientFutures.getFailure;
import static com.jam2in.arcus.http.support.TestClientFutures.getSuccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jam2in.arcus.http.common.adapter.ClientSupport;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import net.spy.memcached.ops.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class KVAdapterTest {

  private ArcusClientPool client;
  private KVAdapter adapter;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    client = mock(ArcusClientPool.class);
    adapter = new KVAdapter(client, new ClientSupport());
  }

  @Test
  @DisplayName("클라이언트 Future의 타임아웃을 사용하고 저장을 재시도하거나 취소하지 않는다")
  @Timeout(5)
  void setUsesClientTimeoutWithoutRetryOrCancellation() {
    // given
    var operation = mock(Operation.class);
    var future = new OperationFuture<Boolean>(new CountDownLatch(1), 1);
    future.setOperation(operation);
    when(client.set("key", 0, "value")).thenReturn(future);

    // expected
    assertThatThrownBy(() -> adapter.set("key", "value", 0))
        .isInstanceOfSatisfying(
            ArcusHttpException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ARCUS_TIMEOUT));
    verify(operation, never()).cancel(any());
    verify(client, times(1)).set("key", 0, "value");
  }

  @Test
  @DisplayName("조회도 클라이언트 GetFuture에 설정된 타임아웃을 따른다")
  @Timeout(5)
  void getUsesClientTimeout() {
    // given
    var operation = mock(Operation.class);
    var future = new GetFuture<Object>(new CountDownLatch(1), 1);
    future.setOperation(operation);
    when(client.asyncGet("key")).thenReturn(future);

    // expected
    assertThatThrownBy(() -> adapter.get("key"))
        .isInstanceOfSatisfying(
            ArcusHttpException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ARCUS_TIMEOUT));
    verify(operation, never()).cancel(any());
  }

  @Test
  @DisplayName("Adapter는 별도 제한 시간 없이 클라이언트 Future의 get을 호출한다")
  void delegatesWaitingToClientFuture() throws Exception {
    // given
    var future = getSuccess("value");
    when(client.asyncGet("key")).thenReturn(future);

    // when
    Object value = adapter.get("key");

    // then
    assertThat(value).isEqualTo("value");
    verify(future).get();
    verify(future, never()).get(anyLong(), any(TimeUnit.class));
  }

  @Test
  @DisplayName("인터럽트 플래그를 복원하고 추가 취소는 수행하지 않는다")
  @SuppressWarnings("unchecked")
  void preservesInterruptWithoutCancellingOperation() throws Exception {
    // given
    GetFuture<Object> future = mock(GetFuture.class);
    when(client.asyncGet("key")).thenReturn(future);
    when(future.get()).thenThrow(new InterruptedException());

    try {
      // expected
      assertThatThrownBy(() -> adapter.get("key"))
          .isInstanceOfSatisfying(
              ArcusHttpException.class,
              e -> assertThat(e.code()).isEqualTo(ErrorCode.ARCUS_UNAVAILABLE));
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
      verify(future, never()).cancel(false);
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  @DisplayName("클라이언트가 취소한 연산을 서비스 불가로 변환한다")
  void mapsCancelledFutureToUnavailable() throws Exception {
    // given
    doReturn(getFailure(new CancellationException())).when(client).asyncGet("key");

    // expected
    assertThatThrownBy(() -> adapter.get("key"))
        .isInstanceOfSatisfying(
            ArcusHttpException.class,
            e -> assertThat(e.code()).isEqualTo(ErrorCode.ARCUS_UNAVAILABLE));
  }

  @Test
  @DisplayName("Future 내부의 타임아웃을 구분한다")
  void mapsExceptionalTimeout() throws Exception {
    // given
    doReturn(getFailure(new TimeoutException())).when(client).asyncGet("key");

    // expected
    assertThatThrownBy(() -> adapter.get("key"))
        .isInstanceOfSatisfying(
            ArcusHttpException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ARCUS_TIMEOUT));
  }

  @Test
  @DisplayName("백엔드 오류 원인을 보존하고 연산 실패로 변환한다")
  void mapsBackendFailure() throws Exception {
    // given
    var cause = new RuntimeException("backend detail");
    doReturn(getFailure(cause)).when(client).asyncGet("key");

    // expected
    assertThatThrownBy(() -> adapter.get("key"))
        .isInstanceOfSatisfying(
            ArcusHttpException.class,
            e -> {
              assertThat(e.code()).isEqualTo(ErrorCode.ARCUS_OPERATION_FAILED);
              assertThat(e.getCause()).isSameAs(cause);
              assertThat(e.getMessage()).doesNotContain("backend detail");
            });
  }

  @ParameterizedTest
  @ValueSource(strings = {"bad key", "bad\nkey", "-bad:prefix", "한글:prefix"})
  @DisplayName("클라이언트의 키 검증 오류를 요청 오류로 변환한다")
  void mapsClientKeyValidationErrors(String key) {
    // given
    when(client.asyncGet(key)).thenThrow(new IllegalArgumentException("invalid key"));

    // expected
    assertThatThrownBy(() -> adapter.get(key))
        .isInstanceOfSatisfying(ArcusHttpException.class,
            exception -> assertThat(exception.code()).isEqualTo(ErrorCode.INVALID_REQUEST));
    verify(client).asyncGet(key);
  }

  @Test
  @DisplayName("최대 바이트 경계의 키는 허용한다")
  void acceptsMaximumKeyBytes() throws Exception {
    // given
    String key = "a".repeat(4000);
    doReturn(getSuccess("v")).when(client).asyncGet(key);

    // when
    Object value = adapter.get(key);

    // then
    assertThat(value).isEqualTo("v");
  }

  @ParameterizedTest
  @MethodSource("nonStringValues")
  @DisplayName("문자열이 아닌 조회 결과도 변환 없이 반환한다")
  void returnsNonStringResult(Object value) throws Exception {
    // given
    doReturn(getSuccess(value)).when(client).asyncGet("key");

    // when
    Object result = adapter.get("key");

    // then
    assertThat(result).isSameAs(value);
  }

  private static Stream<Object> nonStringValues() {
    return Stream.of(42, 3.14, true, Map.of("name", "value"), List.of("value"), new byte[] {1, 2});
  }

}
