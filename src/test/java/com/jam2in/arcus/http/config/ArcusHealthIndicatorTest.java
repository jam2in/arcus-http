package com.jam2in.arcus.http.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.List;
import net.spy.memcached.ArcusClientPool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

class ArcusHealthIndicatorTest {

  @Test
  @DisplayName("캐시 노드에 연결되어 있으면 준비 상태를 반환한다")
  void reportsUpWhenConnected() {
    // given
    var client = mock(ArcusClientPool.class);
    when(client.getAvailableServers()).thenReturn(List.of(new InetSocketAddress("localhost", 11211)));
    when(client.getUnavailableServers()).thenReturn(List.of());
    var indicator = new ArcusHealthIndicator(client);

    // when
    var health = indicator.health();

    // then
    assertThat(health.getStatus()).isEqualTo(Status.UP);
  }

  @Test
  @DisplayName("연결된 캐시 노드가 없으면 준비되지 않은 상태를 반환한다")
  void reportsDownWithoutServers() {
    // given
    var client = mock(ArcusClientPool.class);
    when(client.getAvailableServers()).thenReturn(List.of());
    var indicator = new ArcusHealthIndicator(client);

    // expected
    assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  @DisplayName("일부 캐시 노드와 연결할 수 없으면 준비되지 않은 상태를 반환한다")
  void reportsDownForUnavailableServers() {
    // given
    var client = mock(ArcusClientPool.class);
    when(client.getAvailableServers()).thenReturn(List.of(new InetSocketAddress("localhost", 11211)));
    when(client.getUnavailableServers()).thenReturn(List.of(new InetSocketAddress("localhost", 11212)));
    var indicator = new ArcusHealthIndicator(client);

    // expected
    assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
  }

  @Test
  @DisplayName("연결 상태 확인에 실패해도 내부 예외를 응답에 노출하지 않는다")
  void reportsDownWithoutExposingFailure() {
    // given
    var client = mock(ArcusClientPool.class);
    when(client.getAvailableServers()).thenThrow(new IllegalStateException("private backend detail"));
    var indicator = new ArcusHealthIndicator(client);

    // when
    var health = indicator.health();

    // then
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).isEmpty();
  }
}
