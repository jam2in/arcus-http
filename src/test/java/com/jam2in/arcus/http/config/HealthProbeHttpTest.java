package com.jam2in.arcus.http.config;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;

class HealthProbeHttpTest extends ClientHttpTest {

  @Autowired
  private ApplicationContext context;

  @Autowired
  private ApplicationAvailability availability;

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @DisplayName("캐시 일부 또는 전체 장애는 종합 상태에 반영하고 probe에서 분리한다")
  void isolatesProbesFromCacheOutages(boolean allUnavailable) throws Exception {
    when(client.getAvailableServers()).thenReturn(allUnavailable
        ? List.of() : List.of(new InetSocketAddress("localhost", 11211)));
    when(client.getUnavailableServers()).thenReturn(List.of(new InetSocketAddress("localhost", 11212)));

    mvc.perform(get("/actuator/health"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.status").value("DOWN"));
    mvc.perform(get("/actuator/health/liveness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    mvc.perform(get("/actuator/health/readiness"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  @DisplayName("요청 수신 중단 상태는 readiness에 반영하고 캐시에 접근하지 않는다")
  void reportsRefusingTraffic() throws Exception {
    var previous = availability.getReadinessState();
    try {
      AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);

      mvc.perform(get("/actuator/health/readiness"))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
      mvc.perform(get("/actuator/health/liveness"))
          .andExpect(status().isOk());
      verifyNoInteractions(client);
    } finally {
      AvailabilityChangeEvent.publish(context, previous);
    }
  }

  @Test
  @DisplayName("애플리케이션 생존 실패는 liveness에 반영하고 캐시에 접근하지 않는다")
  void reportsBrokenApplication() throws Exception {
    var previous = availability.getLivenessState();
    try {
      AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);

      mvc.perform(get("/actuator/health/liveness"))
          .andExpect(status().isServiceUnavailable())
          .andExpect(jsonPath("$.status").value("DOWN"));
      verifyNoInteractions(client);
    } finally {
      AvailabilityChangeEvent.publish(context, previous);
    }
  }
}
