package com.jam2in.arcus.http.common.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
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
import com.jam2in.arcus.http.common.exception.GlobalExceptionHandler;
import com.jam2in.arcus.http.kv.controller.KVController;
import com.jam2in.arcus.http.kv.dto.response.GetResponse;
import com.jam2in.arcus.http.kv.dto.response.SetResponse;
import com.jam2in.arcus.http.kv.service.KVService;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = ApiResultHttpTest.TestApplication.class)
@AutoConfigureMockMvc
class ApiResultHttpTest {

  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  @Import({KVController.class, GlobalExceptionHandler.class})
  static class TestApplication {

  }

  @Autowired
  private MockMvc mvc;

  @MockitoBean
  private KVService service;

  @Autowired
  private JsonMapper mapper;

  @Test
  @DisplayName("저장 응답을 성공 여부와 데이터로 감싼다")
  void setReturnsSuccessEnvelope() throws Exception {

    // given
    String requestBody = "{\"key\":\"demo:hello\",\"value\":\"hello\",\"expireTime\":60}";
    String responseBody = "{\"code\":0,\"message\":null,\"content\":{\"stored\":true}}";
    when(service.set("demo:hello", "hello", 60)).thenReturn(new SetResponse(true));

    // when
    mvc.perform(post("/kv/set")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        // then
        .andExpectAll(
            status().isOk(),
            content().json(responseBody)
        );
  }

  @Test
  @DisplayName("조회 응답의 키와 값을 content 안에 반환한다")
  void getReturnsSuccessEnvelope() throws Exception {

    // given
    String requestBody = "{\"key\":\"demo:hello\",\"value\":\"hello\",\"expireTime\":60}";
    String responseBody =
        "{\"code\":0,\"message\":null,\"content\":{\"key\":\"demo:hello\",\"value\":\"안녕하세요\"}}";

    when(service.get("demo:hello"))
        .thenReturn(new GetResponse("demo:hello", "안녕하세요"));

    // when
    mvc.perform(post("/kv/get")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        // then
        .andExpectAll(
            status().isOk(),
            content().json(responseBody)
        );
  }

  @ParameterizedTest
  @CsvSource({
      "INVALID_REQUEST,400", "KEY_NOT_FOUND,404", "VALUE_TYPE_MISMATCH,409",
      "ARCUS_OPERATION_FAILED,502", "ARCUS_UNAVAILABLE,503",
      "ARCUS_TIMEOUT,504", "INTERNAL_ERROR,500"
  })
  @DisplayName("오류 응답을 공통 형식으로 감싸고 HTTP 상태를 보존한다")
  void arcusHttpErrorsPreserveStatusAndEnvelope(ErrorCode code, int httpStatus) throws Exception {

    // given
    String requestBody = "{\"key\":\"error\"}";
    doThrow(new ArcusHttpException(code)).when(service).get("error");

    // when
    mvc.perform(post("/kv/get")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        // then
        .andExpectAll(
            status().is(httpStatus),
            jsonPath("$.code").value(httpStatus),
            jsonPath("$.message").value(code.message()),
            jsonPath("$.content").value(Matchers.nullValue()),
            jsonPath("$.success").doesNotExist(),
            jsonPath("$.error").doesNotExist()
        );
  }

  @Test
  @DisplayName("DTO 검증 실패도 공통 오류 응답으로 반환한다")
  void validationFailureReturnsErrorEnvelope() throws Exception {
    // given
    var request = post("/kv/set")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"value\":\"v\",\"expireTime\":-2}");

    // expected
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(content().json("""
            {"code":400,"message":"The request is invalid.","content":null}
            """));
    verifyNoInteractions(service);
  }

  @Test
  @DisplayName("잘못된 JSON도 공통 오류 응답으로 반환한다")
  void malformedJsonReturnsErrorEnvelope() throws Exception {
    // given
    var request = post("/kv/get")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{");

    // expected
    mvc.perform(request)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(400));
    verifyNoInteractions(service);
  }

  @Test
  @DisplayName("예상하지 못한 예외의 내부 메시지를 숨긴다")
  void unexpectedFailureReturnsSanitizedEnvelope() throws Exception {
    // given
    when(service.get("k")).thenThrow(new RuntimeException("private backend details"));

    // expected
    mvc.perform(post("/kv/get")
            .contentType(MediaType.APPLICATION_JSON).content("{\"key\":\"k\"}"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().json("""
            {"code":500,"message":"An unexpected error occurred.","content":null}
            """));
  }

  @Test
  @DisplayName("메서드와 미디어 타입 오류도 상태를 유지하며 감싼다")
  void protocolFailuresReturnErrorEnvelope() throws Exception {
    // given
    var wrongMethodRequest = get("/kv/get");
    var unsupportedMediaRequest = post("/kv/set")
        .contentType(MediaType.TEXT_PLAIN).content("hello");

    // expected
    mvc.perform(wrongMethodRequest)
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value(405));
    mvc.perform(unsupportedMediaRequest)
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value(415));
    verifyNoInteractions(service);
  }

  @Test
  @DisplayName("JSON을 수락하지 않는 요청의 406 빈 본문을 보존한다")
  void notAcceptableResponseRemainsEmpty() throws Exception {
    // given
    when(service.get("k")).thenReturn(new GetResponse("k", "value"));
    var request = post("/kv/get")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.TEXT_HTML)
        .content("{\"key\":\"k\"}");

    // expected
    mvc.perform(request)
        .andExpect(status().isNotAcceptable())
        .andExpect(content().string(""));
    verify(service).get("k");
  }

  @ParameterizedTest
  @CsvSource({
      "application/json,application/json",
      "application/vnd.arcus+json,application/vnd.arcus+json",
      "*/*,application/json"
  })
  @DisplayName("Accept와 JSON 변환기로 응답 형식을 선택한다")
  void negotiatesJsonResponseType(String accept, String responseType) throws Exception {
    // given
    when(service.get("k")).thenReturn(new GetResponse("k", "value"));
    var request = post("/kv/get")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(accept)
        .content("{\"key\":\"k\"}");

    // expected
    mvc.perform(request)
        .andExpect(status().isOk())
        .andExpect(content().contentType(responseType))
        .andExpect(jsonPath("$.content.value").value("value"));
    verify(service).get("k");
  }

  @Test
  @DisplayName("지원하지 않는 Accept는 저장 실행 후 응답 변환 시 406을 반환한다")
  void rejectsUnsupportedAcceptAfterExecutingStore() throws Exception {
    // given
    when(service.set("k", "value", 0)).thenReturn(new SetResponse(true));
    var request = post("/kv/set")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.TEXT_HTML)
        .content("{\"key\":\"k\",\"value\":\"value\",\"expireTime\":0}");

    // expected
    mvc.perform(request)
        .andExpect(status().isNotAcceptable())
        .andExpect(content().string(""));
    verify(service).set("k", "value", 0);
  }

  @Test
  @DisplayName("부가 데이터가 있는 오류도 동일한 형식으로 직렬화한다")
  void errorPreservesAdditionalContent() {
    // given
    ApiResult<Map<String, String>> result =
        ApiResult.error(400, "Invalid field.", Map.of("field", "key"));

    // expected
    assertThat(mapper.readTree(mapper.writeValueAsString(result)))
        .isEqualTo(mapper.readTree("""
            {"code":400,"message":"Invalid field.","content":{"field":"key"}}
            """));
  }

  @Test
  @DisplayName("OpenAPI에 공통 응답과 content의 구체 타입을 표시한다")
  void openApiDocumentsEnvelopeAndDataTypes() throws Exception {
    // given
    var request = get("/v3/api-docs");

    // expected
    mvc.perform(request)
        .andExpect(status().isOk())
        .andExpect(jsonPath(
            "$.paths['/kv/set'].post.responses['200'].content['application/json'].schema['$ref']")
            .value("#/components/schemas/ApiResultSetResponse"))
        .andExpect(jsonPath(
            "$.paths['/kv/get'].post.responses['200'].content['application/json'].schema['$ref']")
            .value("#/components/schemas/ApiResultGetResponse"))
        .andExpect(jsonPath("$.components.schemas.ApiResultSetResponse.properties.content['$ref']")
            .value("#/components/schemas/SetResponse"))
        .andExpect(jsonPath("$.components.schemas.ApiResultGetResponse.properties.content['$ref']")
            .value("#/components/schemas/GetResponse"))
        .andExpect(jsonPath("$.components.schemas.ApiResultGetResponse.properties.code.type")
            .value("integer"))
        .andExpect(jsonPath(
            "$.paths['/kv/get'].post.responses['404'].content['application/json'].schema['$ref']")
            .value("#/components/schemas/ApiResult"))
        .andExpect(jsonPath("$.components.schemas.ApiResult.properties.message.type")
            .value("string"))
        .andExpect(jsonPath("$.components.schemas.ApiResult.properties.content").exists())
        .andExpect(jsonPath("$.components.schemas.ApiResult.properties.success").doesNotExist())
        .andExpect(jsonPath("$.components.schemas.ApiResult.properties.error").doesNotExist());
  }
}
