package com.jam2in.arcus.http.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jam2in.arcus.http.common.adapter.ClientSupport;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import net.spy.memcached.internal.CompositeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ClientSupportTest {

  @Test
  @DisplayName("취소된 Future를 연결 불가로 변환하고 재시도하지 않는다")
  @SuppressWarnings("unchecked")
  void handlesCancelledFutureWithoutRetry() throws Exception {
    // given
    Future<Boolean> future = mock(Future.class);
    when(future.get()).thenThrow(new CancellationException());
    var support = new ClientSupport();

    // when
    var error = catchThrowableOfType(ArcusHttpException.class, () -> support.call(() -> future));

    // then
    assertThat(error.code()).isEqualTo(ErrorCode.ARCUS_UNAVAILABLE);
    verify(future, times(1)).get();
    verifyNoMoreInteractions(future);
  }

  @ParameterizedTest
  @MethodSource("bulkFailures")
  @DisplayName("bulk 취소와 혼합 오류 및 타임아웃을 구분하고 재시도하지 않는다")
  @SuppressWarnings("unchecked")
  void classifiesBulkFailures(ExecutionException failure, ErrorCode expected) throws Exception {
    // given
    Future<Object> future = mock(Future.class);
    when(future.get()).thenThrow(failure);
    var support = new ClientSupport();

    // when
    var error = catchThrowableOfType(ArcusHttpException.class, () -> support.call(() -> future));

    // then
    assertThat(error.code()).isEqualTo(expected);
    verify(future, times(1)).get();
    verifyNoMoreInteractions(future);
  }

  private static Stream<Arguments> bulkFailures() {
    return Stream.of(
        Arguments.of(new CompositeException(List.of(new CancellationException())),
            ErrorCode.ARCUS_UNAVAILABLE),
        Arguments.of(new CompositeException(List.of(
                new CancellationException(), new CancellationException())),
            ErrorCode.ARCUS_UNAVAILABLE),
        Arguments.of(new CompositeException(List.of(
                new CancellationException(), new RuntimeException("operation failed"))),
            ErrorCode.ARCUS_OPERATION_FAILED),
        Arguments.of(new CompositeException(List.of(new RuntimeException("operation failed"))),
            ErrorCode.ARCUS_OPERATION_FAILED),
        Arguments.of(new ExecutionException(new TimeoutException()), ErrorCode.ARCUS_TIMEOUT));
  }
}
