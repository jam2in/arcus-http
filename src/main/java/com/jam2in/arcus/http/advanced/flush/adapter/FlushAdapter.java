package com.jam2in.arcus.http.advanced.flush.adapter;

import com.jam2in.arcus.http.common.adapter.ClientSupport;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.ArcusClientPool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FlushAdapter {

  private final ArcusClientPool client;
  private final ClientSupport support;

  public boolean flush(String prefix, Integer delay) {
    return support.call(() -> prefix == null
        ? (delay == null ? client.flush() : client.flush(delay))
        : (delay == null ? client.flush(prefix) : client.flush(prefix, delay)));
  }
}
