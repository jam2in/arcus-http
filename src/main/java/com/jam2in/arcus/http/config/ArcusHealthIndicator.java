package com.jam2in.arcus.http.config;

import lombok.RequiredArgsConstructor;
import net.spy.memcached.ArcusClientPool;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArcusHealthIndicator implements HealthIndicator {

  private final ArcusClientPool client;

  @Override
  public Health health() {
    try {
      if (client.getAvailableServers().isEmpty() || !client.getUnavailableServers().isEmpty()) {
        return Health.down().build();
      }
      return Health.up().build();
    } catch (RuntimeException exception) {
      return Health.down().build();
    }
  }
}
