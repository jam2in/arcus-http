package com.jam2in.arcus.http.map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.ops.CollectionOperationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

class MapPipedHttpTest extends ClientHttpTest {

  @Autowired
  private JsonMapper mapper;

  @ParameterizedTest
  @CsvSource({"POST,/map/piped-insert-bulk", "PATCH,/map/piped-update-bulk"})
  @DisplayName("Map piped는 500개까지 입력 순서와 실패 위치를 보존한다")
  void acceptsMaximumBatch(String method, String path) throws Exception {
    // given
    var elements = elements(500);
    var failures = Map.of(499,
        new CollectionOperationStatus(false, "OVERFLOWED", CollectionResponse.OVERFLOWED));
    var future = collection(failures, CollectionResponse.FAILED_END);
    if (method.equals("POST")) {
      when(client.asyncMopPipedInsertBulk(eq("k"), anyMap(), isNull())).thenReturn(future);
    } else {
      when(client.asyncMopPipedUpdateBulk(eq("k"), anyMap())).thenReturn(future);
    }

    // expected
    mvc.perform(request(HttpMethod.valueOf(method), path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("key", "k", "elements", elements))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.complete").value(false))
        .andExpect(jsonPath("$.content.failures.499").value("OVERFLOWED"));
    var expectedKeys = new ArrayList<>(elements.keySet());
    if (method.equals("POST")) {
      verify(client).asyncMopPipedInsertBulk(eq("k"),
          argThat(actual -> new ArrayList<>(actual.keySet()).equals(expectedKeys)), isNull());
    } else {
      verify(client).asyncMopPipedUpdateBulk(eq("k"),
          argThat(actual -> new ArrayList<>(actual.keySet()).equals(expectedKeys)));
    }
  }

  @ParameterizedTest
  @CsvSource({"POST,/map/piped-insert-bulk", "PATCH,/map/piped-update-bulk"})
  @DisplayName("Map piped는 501개부터 클라이언트 호출 전에 거부한다")
  void rejectsOversizedBatch(String method, String path) throws Exception {
    // expected
    mvc.perform(request(HttpMethod.valueOf(method), path)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("key", "k", "elements", elements(501)))))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(client);
  }

  private Map<String, Object> elements(int count) {
    var elements = new LinkedHashMap<String, Object>();
    for (int i = 0; i < count; i++) {
      elements.put("field-" + i, "value-" + i);
    }
    return elements;
  }
}
