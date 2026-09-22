package com.jam2in.arcus.http;

import static com.jam2in.arcus.http.support.TestClientFutures.getFailure;
import static com.jam2in.arcus.http.support.TestClientFutures.getSuccess;
import static com.jam2in.arcus.http.support.TestClientFutures.storeSuccess;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.OperationTimeoutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "arcus.operation-timeout-ms=50")
@AutoConfigureMockMvc
class ArcusHttpApplicationTests {

  @Autowired
  MockMvc mvc;

  @MockitoBean
  ArcusClientPool client;

  @Test
  @DisplayName("Controller의 매핑과 바인딩으로 문자열을 저장한다")
  void setStoresStringThroughClient() throws Exception {
    // given
    doReturn(storeSuccess(true)).when(client).set("demo:hello", 60, "안녕하세요");

    // expected
    mvc.perform(post("/kv/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"demo:hello\",\"value\":\"안녕하세요\",\"expireTime\":60}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.stored").value(true));
    verify(client).set("demo:hello", 60, "안녕하세요");
  }

  @Test
  @DisplayName("저장한 문자열을 키와 함께 조회한다")
  void getReturnsStoredValue() throws Exception {
    // given
    doReturn(getSuccess("안녕하세요")).when(client).asyncGet("demo:hello");

    // expected
    mvc.perform(
            post("/kv/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"demo:hello\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.key").value("demo:hello"))
        .andExpect(jsonPath("$.content.value").value("안녕하세요"));
  }

  @Test
  @DisplayName("빈 문자열과 만료 없음도 저장할 수 있다")
  void setAcceptsEmptyValueAndZeroTtl() throws Exception {
    // given
    doReturn(storeSuccess(true)).when(client).set("empty", 0, "");

    // expected
    mvc.perform(
            post("/kv/set")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"empty\",\"value\":\"\",\"expireTime\":0}"))
        .andExpect(status().isOk());
    verify(client).set("empty", 0, "");
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 1, 2592000, 2592001, Integer.MAX_VALUE})
  @DisplayName("상대 TTL과 절대 만료 시각 및 특수값을 변환 없이 전달한다")
  void setPreservesExpiration(int expireTime) throws Exception {
    // given
    doReturn(storeSuccess(true)).when(client).set("ttl", expireTime, "v");

    // expected
    mvc.perform(
            post("/kv/set")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"ttl\",\"value\":\"v\",\"expireTime\":" + expireTime + "}"))
        .andExpect(status().isOk());
    verify(client).set("ttl", expireTime, "v");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
          "{}",
          "null",
          "{",
          "[]",
          "{\"key\":\"k\",\"value\":\"v\"}",
          "{\"key\":\"k\",\"value\":null,\"expireTime\":0}",
          "{\"key\":\"\",\"value\":\"v\",\"expireTime\":0}",
          "{\"key\":\"k\",\"value\":\"v\",\"expireTime\":-2}",
          "{\"key\":\"k\",\"value\":\"v\",\"expireTime\":2147483648}",
          "{\"key\":\"k\",\"value\":\"v\",\"expireTime\":1.5}",
          "{\"key\":\"k\",\"value\":\"v\",\"expireTime\":\"60\"}",
          "{\"key\":123,\"value\":\"v\",\"expireTime\":0}",
          "{\"key\":\"k\",\"value\":\"v\",\"expireTime\":0,\"serviceCode\":\"other\"}"
      })
  @DisplayName("잘못된 HTTP 저장 요청을 클라이언트 호출 전에 거부한다")
  void setRejectsInvalidInputBeforeCallingArcus(String json) throws Exception {
    // given
    var request = post("/kv/set")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json);

    // expected
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
    verifyNoInteractions(client);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
          "{}",
          "{\"key\":null}",
          "{\"key\":\" \"}",
          "{\"key\":123}",
      })
  @DisplayName("조회 요청에도 Controller의 검증을 적용한다")
  void getRejectsInvalidInput(String json) throws Exception {
    // given
    var request = post("/kv/get")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json);

    // expected
    mvc.perform(request)
        .andExpect(status().isBadRequest());
    verifyNoInteractions(client);
  }

  @Test
  @DisplayName("클라이언트의 키 바이트 크기 오류를 HTTP 400으로 변환한다")
  void mapsOversizedUnicodeKeyError() throws Exception {
    // given
    String key = "가".repeat(1334);
    when(client.asyncGet(key)).thenThrow(new IllegalArgumentException("key is too long"));

    // expected
    mvc.perform(post("/kv/get")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"" + key + "\"}"))
        .andExpect(status().isBadRequest());
    verify(client).asyncGet(key);
  }

  @ParameterizedTest
  @ValueSource(strings = {"bad key", "-bad:prefix"})
  @DisplayName("저장과 조회의 클라이언트 키 오류를 HTTP 400으로 변환한다")
  void mapsClientKeyErrorsToBadRequest(String key) throws Exception {
    // given
    when(client.set(key, 0, "v")).thenThrow(new IllegalArgumentException("invalid key"));
    when(client.asyncGet(key)).thenThrow(new IllegalArgumentException("invalid key"));

    // expected
    mvc.perform(post("/kv/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"" + key + "\",\"value\":\"v\",\"expireTime\":0}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/kv/get")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"" + key + "\"}"))
        .andExpect(status().isBadRequest());
    verify(client).set(key, 0, "v");
    verify(client).asyncGet(key);
  }

  @Test
  @DisplayName("원본 문자열 크기는 서버에서 제한하지 않고 기본 Transcoder에 맡긴다")
  void delegatesValueSizePolicyToClient() throws Exception {
    // given
    String value = "가".repeat(349526);
    doReturn(storeSuccess(true)).when(client).set("large", 0, value);

    // expected
    mvc.perform(post("/kv/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"large\",\"value\":\"" + value + "\",\"expireTime\":0}"))
        .andExpect(status().isOk());
    verify(client).set("large", 0, value);
  }

  @Test
  @DisplayName("존재하지 않는 키는 404로 반환한다")
  void getReturnsNotFoundForCacheMiss() throws Exception {
    // given
    doReturn(getSuccess(null)).when(client).asyncGet("missing");

    // expected
    mvc.perform(
            post("/kv/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"missing\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404));
  }

  @Test
  @DisplayName("빈 문자열 조회는 미존재로 처리하지 않는다")
  void getPreservesEmptyString() throws Exception {
    // given
    doReturn(getSuccess("")).when(client).asyncGet("empty");

    // expected
    mvc.perform(
            post("/kv/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"empty\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value").value(""));
  }

  @Test
  @DisplayName("저장 거부를 성공으로 응답하지 않는다")
  void setReportsRejectedStore() throws Exception {
    // given
    doReturn(storeSuccess(false)).when(client).set(any(), anyInt(), any());

    // expected
    mvc.perform(
            post("/kv/set")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"k\",\"value\":\"v\",\"expireTime\":0}"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value(502));
  }

  @Test
  @DisplayName("연결 실패의 내부 메시지를 응답에 노출하지 않는다")
  void getSanitizesConnectionFailure() throws Exception {
    // given
    when(client.asyncGet("k")).thenThrow(new IllegalStateException("private backend address"));

    // expected
    mvc.perform(post("/kv/get")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(503))
        .andExpect(jsonPath("$.message").value("Arcus is unavailable."));
  }

  @Test
  @DisplayName("클라이언트가 보고한 타임아웃을 504로 응답한다")
  void getReportsClientTimeout() throws Exception {
    // given
    doReturn(getFailure(new OperationTimeoutException("client timeout"))).when(client)
        .asyncGet("slow");

    // expected
    mvc.perform(
            post("/kv/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"slow\"}"))
        .andExpect(status().isGatewayTimeout())
        .andExpect(jsonPath("$.code").value(504));
  }

  @Test
  @DisplayName("지원하지 않는 저장 값은 409로 구분한다")
  void getRejectsUnsupportedStoredValue() throws Exception {
    // given
    doReturn(getFailure(new ArcusHttpException(ErrorCode.VALUE_TYPE_MISMATCH))).when(client)
        .asyncGet("typed");

    // expected
    mvc.perform(
            post("/kv/get")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"key\":\"typed\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(409));
  }

  @Test
  @DisplayName("잘못된 메서드와 미디어 타입의 HTTP 상태를 보존한다")
  void preservesProtocolErrorStatuses() throws Exception {
    // given
    var wrongMethodRequest = get("/kv/get");
    var unsupportedMediaRequest = post("/kv/set")
        .contentType(MediaType.TEXT_PLAIN).content("hello");

    // expected
    mvc.perform(wrongMethodRequest)
        .andExpect(status().isMethodNotAllowed());
    mvc.perform(unsupportedMediaRequest)
        .andExpect(status().isUnsupportedMediaType());
    verifyNoInteractions(client);
  }

  @Test
  @DisplayName("JSON을 수락하지 않는 요청에는 변환을 재시도하지 않고 406을 반환한다")
  void returnsEmptyNotAcceptableResponse() throws Exception {
    // given
    doReturn(getSuccess("value")).when(client).asyncGet("k");
    var request = post("/kv/get")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.TEXT_HTML)
        .content("{\"key\":\"k\"}");

    // expected
    mvc.perform(request)
        .andExpect(status().isNotAcceptable())
        .andExpect(content().string(""));
    verify(client).asyncGet("k");
  }

  @Test
  @DisplayName("Controller의 매핑과 Spec의 설명 및 응답 스키마를 OpenAPI에 반영한다")
  void documentsControllerSpecInOpenApi() throws Exception {
    // given
    var request = get("/v3/api-docs");

    // expected
    mvc.perform(request)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/kv/set'].post").exists())
        .andExpect(jsonPath("$.paths['/kv/get'].post").exists())
        .andExpect(jsonPath("$.paths['/kv/get'].get").doesNotExist())
        .andExpect(jsonPath("$.paths['/kv/set'].post.summary").value("Key-Value 저장"))
        .andExpect(
            jsonPath(
                "$.paths['/kv/set'].post.requestBody.content['application/json'].schema['$ref']")
                .value("#/components/schemas/SetRequest"))
        .andExpect(
            jsonPath(
                "$.components.schemas.SetRequest.required", hasItems("key", "value", "expireTime")))
        .andExpect(
            jsonPath("$.components.schemas.SetRequest.properties.expireTime.maximum")
                .doesNotExist())
        .andExpect(jsonPath("$.components.schemas.SetRequest.properties.expireTime.minimum")
            .value(-1))
        .andExpect(
            jsonPath(
                "$.paths['/kv/get'].post.responses['404'].content['application/json'].schema['$ref']")
                .value("#/components/schemas/ApiResult"))
        .andExpect(
            jsonPath(
                "$.paths['/kv/set'].post.responses['200'].content['application/json'].schema['$ref']")
                .value("#/components/schemas/ApiResultSetResponse"));
  }
}
