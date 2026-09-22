package com.jam2in.arcus.http.common;

import static com.jam2in.arcus.http.support.TestClientFutures.storeSuccess;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

class ValueEncodingHttpTest extends ClientHttpTest {

  @Autowired
  private JsonMapper mapper;

  @Test
  @DisplayName("기본 트랜스코더의 KV 크기 초과 오류는 HTTP 400으로 반환한다")
  void rejectsOversizedKvValue() throws Exception {
    var transcoder = SerializingTranscoder.forKV().build();
    when(client.set(eq("k"), eq(0), any())).thenAnswer(invocation -> {
      transcoder.encode(invocation.getArgument(2));
      return storeSuccess(true);
    });
    var bytes = new byte[CachedData.MAX_SIZE * 2];
    new Random(1).nextBytes(bytes);
    var value = Base64.getEncoder().encodeToString(bytes);

    mvc.perform(post("/kv/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("key", "k", "expireTime", 0, "value", value))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.message").value("The request is invalid."));
  }

  @Test
  @DisplayName("기본 트랜스코더의 컬렉션 직렬화 크기 초과도 HTTP 400으로 반환한다")
  void rejectsOversizedCollectionValue() throws Exception {
    var transcoder = SerializingTranscoder.forCollection()
        .forceJDKSerializationForCollection().build();
    when(client.asyncLopInsert(eq("k"), eq(0), any(), isNull())).thenAnswer(invocation -> {
      transcoder.encode(invocation.getArgument(2));
      return null;
    });
    var value = "a".repeat(SerializingTranscoder.MAX_COLLECTION_ELEMENT_SIZE);

    mvc.perform(post("/list/insert")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("key", "k", "index", 0, "value", value))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
  }

  @Test
  @DisplayName("원본이 커도 기본 트랜스코더의 압축 후 한도 이내이면 허용한다")
  void acceptsLargeCompressibleValue() throws Exception {
    var transcoder = SerializingTranscoder.forKV().build();
    when(client.set(eq("k"), eq(0), any())).thenAnswer(invocation -> {
      transcoder.encode(invocation.getArgument(2));
      return storeSuccess(true);
    });
    var value = "a".repeat(CachedData.MAX_SIZE + 1);

    mvc.perform(post("/kv/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("key", "k", "expireTime", 0, "value", value))))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("압축 처리 예외는 입력 오류로 바꾸지 않고 HTTP 500으로 반환한다")
  void reportsCompressionFailureAsInternalError() throws Exception {
    when(client.set("k", 0, "value")).thenThrow(
        new RuntimeException("IO exception compressing data", new IOException("private detail")));

    mvc.perform(post("/kv/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\",\"expireTime\":0,\"value\":\"value\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(500))
        .andExpect(jsonPath("$.message").value("An unexpected error occurred."));
  }
}
