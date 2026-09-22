package com.jam2in.arcus.http.advanced.flush;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.concurrent.CompletableFuture;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.internal.OperationFuture;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class FlushHttpTest extends ClientHttpTest {

  @Test
  @DisplayName("전체·prefix Flush와 지연 시간을 구분해서 호출한다")
  @SuppressWarnings("unchecked")
  void selectsFlushOverloads() throws Exception {
    // given
    when(client.flush()).thenReturn(CompletableFuture.completedFuture(true));
    when(client.flush(5)).thenReturn(CompletableFuture.completedFuture(true));
    OperationFuture<Boolean> prefixed = mock(OperationFuture.class);
    when(prefixed.get()).thenReturn(false);
    when(client.flush("demo")).thenReturn(prefixed);
    when(client.flush("demo", 5)).thenReturn(prefixed);
    var requests = new String[]{
        "{}",
        "{\"delay\":5}",
        "{\"prefix\":\"demo\"}",
        "{\"prefix\":\"demo\",\"delay\":5}"
    };

    // expected
    for (String json : requests) {
      mvc.perform(delete("/cache/flush")
              .contentType(MediaType.APPLICATION_JSON)
              .content(json))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value(0));
    }
    verify(client).flush();
    verify(client).flush(5);
    verify(client).flush("demo");
    verify(client).flush("demo", 5);
  }

  @Test
  @DisplayName("잘못된 prefix와 음수 지연 시간은 서버에 전송하지 않는다")
  void rejectsInvalidFlushParameters() throws Exception {
    // given
    var requests = new String[]{
        "{\"prefix\":\"bad prefix\"}",
        "{\"prefix\":\"a:b\"}",
        "{\"delay\":-1}"
    };

    // expected
    for (String json : requests) {
      mvc.perform(delete("/cache/flush")
              .contentType(MediaType.APPLICATION_JSON)
              .content(json))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(client);
  }

  @Test
  @DisplayName("Flush 타임아웃은 완료 여부 불명확으로 응답하고 재시도하지 않는다")
  void preservesUnknownOutcomeOnTimeout() throws Exception {
    // given
    when(client.flush()).thenThrow(new OperationTimeoutException("timeout"));

    // expected
    mvc.perform(delete("/cache/flush")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isGatewayTimeout())
        .andExpect(jsonPath("$.message").value(Matchers.containsString("unknown")));
    verify(client, times(1)).flush();
  }

  @Test
  @DisplayName("빈 prefix는 prefix 없는 아이템 전용 Flush를 호출한다")
  @SuppressWarnings("unchecked")
  void flushesUnprefixedItems() throws Exception {
    // given
    OperationFuture<Boolean> future = mock(OperationFuture.class);
    when(future.get()).thenReturn(true);
    when(client.flush("")).thenReturn(future);

    // expected
    mvc.perform(delete("/cache/flush")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
        {"prefix":""}
        """))
        .andExpect(status().isOk());
    verify(client).flush("");
    verify(client, never()).flush();
  }
}
