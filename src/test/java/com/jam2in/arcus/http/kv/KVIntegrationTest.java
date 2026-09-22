package com.jam2in.arcus.http.kv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.response.ApiResult;
import com.jam2in.arcus.http.kv.dto.response.GetResponse;
import com.jam2in.arcus.http.kv.dto.response.SetResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KVIntegrationTest {

  private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(5);

  @LocalServerPort
  private int port;

  @Autowired
  private JsonMapper mapper;

  @Autowired
  private ArcusClientPool client;

  private final List<String> keys = new ArrayList<>();

  private final HttpClient http = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();

  @AfterEach
  void cleanUp() throws Exception {
    try {
      // Delete only this test's UUID keys, including keys stored with expireTime=0.
      for (String key : keys) {
        client.delete(key).get(5, TimeUnit.SECONDS);
      }
    } finally {
      http.close();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"hello", "안녕하세요 🙂", ""})
  @DisplayName("HTTP로 저장한 문자열을 실제 Arcus에서 조회한다")
  void storesAndReadsStringsThroughHttp(String value) throws Exception {
    // given
    String key = newKey();

    // expected
    store(key, value, 60);
    assertStoredValue(key, value);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "123", "2147483648", "3.25", "true", "{}", "[]",
      "{\"name\":\"사용자\",\"items\":[1,true,null]}", "[1,\"1\",false,{\"a\":1}]"
  })
  @DisplayName("JSON 값을 HTTP와 기본 Java Transcoder로 구조와 타입을 보존해 조회한다")
  void roundTripsJsonValues(String json) throws Exception {
    // given
    String key = newKey();
    Object value = mapper.readValue(json, Object.class);

    // expected
    store(key, value, 60);
    assertStoredValue(key, value);
    assertThat(client.asyncGet(key, new SerializingTranscoder()).get()).isEqualTo(value);
  }

  @Test
  @DisplayName("Object 값의 조건부 저장과 CAS 및 bulk 조회에서 값을 보존한다")
  void preservesObjectsAcrossStoreAndReadVariants() throws Exception {
    // given
    String key = newKey();
    String secondKey = newKey();
    String missingKey = newKey();
    var original = List.of(1, "1", true);
    var replacement = Map.of("items", original);

    // expected
    assertThat(post("/kv/add", Map.of("key", key, "value", original, "expireTime", 60))
        .statusCode()).isEqualTo(200);
    var replace = HttpRequest.newBuilder(uri("/kv/replace"))
        .timeout(HTTP_TIMEOUT)
        .header("Content-Type", "application/json")
        .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(
            Map.of("key", key, "value", replacement, "expireTime", 60))))
        .build();
    assertThat(http.send(replace, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
    assertStoredValue(key, replacement);
    var versioned = mapper.readTree(post("/kv/gets", Map.of("key", key)).body());
    assertThat(versioned.at("/content/value")).isEqualTo(mapper.valueToTree(replacement));
    assertThat(versioned.at("/content/cas").isIntegralNumber()).isTrue();

    var values = Map.of(key, replacement, secondKey, original);
    assertThat(post("/kv/store-bulk", Map.of("mode", "SET", "expireTime", 60,
        "values", values)).statusCode()).isEqualTo(200);
    var query = Map.of("keys", List.of(key, secondKey, missingKey));
    var bulk = mapper.readTree(post("/kv/get-bulk", query).body()).path("content");
    assertThat(bulk.path("values")).isEqualTo(mapper.valueToTree(values));
    assertThat(bulk.path("missingKeys")).isEqualTo(mapper.valueToTree(List.of(missingKey)));
    var casBulk = mapper.readTree(post("/kv/gets-bulk", query).body()).path("content");
    assertThat(casBulk.path("values").path(secondKey).path("value"))
        .isEqualTo(mapper.valueToTree(original));
    assertThat(casBulk.path("missingKeys")).isEqualTo(mapper.valueToTree(List.of(missingKey)));

    assertThat(post("/kv/store-bulk", Map.of("mode", "REPLACE", "expireTime", 60,
        "keys", List.of(key, secondKey), "value", original)).statusCode()).isEqualTo(200);
    var touch = HttpRequest.newBuilder(uri("/kv/get-and-touch"))
        .timeout(HTTP_TIMEOUT)
        .header("Content-Type", "application/json")
        .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(
            Map.of("key", key, "expireTime", 120))))
        .build();
    var touched = http.send(touch, HttpResponse.BodyHandlers.ofString());
    assertThat(touched.statusCode()).as(touched.body()).isEqualTo(200);
    assertThat(mapper.readTree(touched.body()).at("/content/value"))
        .isEqualTo(mapper.valueToTree(original));
  }

  @Test
  @DisplayName("같은 키에 다시 저장하면 실제 값이 변경된다")
  void overwritesExistingValue() throws Exception {
    // given
    String key = newKey();
    store(key, "before", 60);

    // expected
    store(key, "after", 60);
    assertStoredValue(key, "after");
  }

  @Test
  @DisplayName("실제 Arcus에 없는 키는 HTTP 404로 응답한다")
  void reportsMissingKey() throws Exception {
    // given
    String key = newKey();

    // expected
    assertError(post("/kv/get", Map.of("key", key)), 404, ErrorCode.KEY_NOT_FOUND);
  }

  @Test
  @DisplayName("실제 Arcus의 TTL이 지나면 저장한 키를 조회할 수 없다")
  void expiresStoredValue() throws Exception {
    // given
    String key = newKey();
    store(key, "expires", 2);
    assertStoredValue(key, "expires");

    // expected
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(6);
    while (System.nanoTime() < deadline) {
      HttpResponse<String> response = post("/kv/get", Map.of("key", key));
      if (response.statusCode() == 404) {
        assertError(response, 404, ErrorCode.KEY_NOT_FOUND);
        return;
      }
      assertThat(response.statusCode()).isEqualTo(200);
      Thread.sleep(100);
    }
    fail("The stored key did not expire within 6 seconds.");
  }

  @ParameterizedTest
  @ValueSource(longs = {0, 2592000})
  @DisplayName("만료 없음과 30일 TTL 경계값을 실제 서버가 수락한다")
  void acceptsExpirationBoundaries(long expireTime) throws Exception {
    // given
    String key = newKey();

    // expected
    store(key, "boundary", expireTime);
    assertStoredValue(key, "boundary");
  }

  @Test
  @DisplayName("30일 이후의 Unix timestamp로 저장하면 남은 TTL이 유지된다")
  void storesWithAbsoluteExpiration() throws Exception {
    // given
    String key = newKey();
    long ttl = Duration.ofDays(31).toSeconds();
    long expireTime = Instant.now().getEpochSecond() + ttl;

    // expected
    store(key, "absolute", expireTime);
    assertStoredValue(key, "absolute");
    assertThat(client.asyncGetAttr(key).get().getExpireTime().longValue())
        .isBetween(ttl - 10, ttl + 1);
  }

  @Test
  @DisplayName("30일 경계를 넘는 과거 timestamp는 서버에서 즉시 만료된다")
  void expiresPastAbsoluteTimestamp() throws Exception {
    // given
    String key = newKey();

    // expected
    store(key, "expired", 2592001);
    assertError(post("/kv/get", Map.of("key", key)), 404, ErrorCode.KEY_NOT_FOUND);
  }

  @Test
  @DisplayName("다른 클라이언트가 저장한 숫자 값을 문자열로 잘못 반환하지 않는다")
  void readsNativeNumberFlags() throws Exception {
    // given
    String key = newKey();
    assertThat(client.set(key, 60, 42, new SerializingTranscoder()).get(5, TimeUnit.SECONDS))
        .isTrue();

    // expected
    assertStoredValue(key, 42);
  }

  @Test
  @DisplayName("잘못된 TTL 요청은 실제 캐시에 저장되지 않는다")
  void rejectsInvalidValuesWithoutStoring() throws Exception {
    // given
    String key = newKey();

    // expected
    assertError(
        post("/kv/set", Map.of("key", key, "value", "value", "expireTime", -2)),
        400,
        ErrorCode.INVALID_REQUEST);
    assertError(post("/kv/get", Map.of("key", key)), 404, ErrorCode.KEY_NOT_FOUND);
  }

  @ParameterizedTest
  @MethodSource("invalidKeys")
  @DisplayName("실제 클라이언트의 키 검증 오류를 HTTP 400으로 반환한다")
  void mapsRealClientKeyErrors(String invalidKey) throws Exception {
    // given
    String validKey = newKey();

    // expected
    assertError(post("/kv/set", Map.of("key", invalidKey, "value", "v", "expireTime", 60)),
        400, ErrorCode.INVALID_REQUEST);
    assertError(post("/kv/get", Map.of("key", invalidKey)), 400, ErrorCode.INVALID_REQUEST);
    assertError(post("/kv/store-bulk", Map.of("mode", "SET", "expireTime", 60,
        "keys", List.of(validKey, invalidKey), "value", "v")), 400, ErrorCode.INVALID_REQUEST);
    assertError(post("/kv/get", Map.of("key", validKey)), 404, ErrorCode.KEY_NOT_FOUND);
  }

  static Stream<String> invalidKeys() {
    return Stream.of("bad key", "bad\nkey", "-bad:prefix", "한글:prefix", "가".repeat(1334));
  }

  @Test
  @DisplayName("실행 중인 애플리케이션이 KV OpenAPI 문서를 제공한다")
  void servesOpenApiDocument() throws Exception {
    // given
    String path = "/v3/api-docs";

    // when
    HttpResponse<String> response = get(path);

    // then
    assertThat(response.statusCode()).isEqualTo(200);
    var document = mapper.readTree(response.body());
    assertThat(document.at("/paths/~1kv~1set/post/responses/200").isMissingNode()).isFalse();
    assertThat(document.at("/paths/~1kv~1get/post/responses/404").isMissingNode()).isFalse();
  }

  @Test
  @DisplayName("실행 중인 애플리케이션이 Swagger UI를 제공한다")
  void servesSwaggerUi() throws Exception {
    // given
    String path = "/swagger-ui/index.html";

    // when
    HttpResponse<String> response = get(path);

    // then
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).contains("swagger-ui");
  }

  private String newKey() {
    String key = "arcus-http-junit:" + UUID.randomUUID();
    keys.add(key);
    return key;
  }

  private void store(String key, Object value, long expireTime) throws Exception {
    HttpResponse<String> response =
        post("/kv/set", Map.of("key", key, "value", value, "expireTime", expireTime));
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    assertThat(mapper.readValue(response.body(), new TypeReference<ApiResult<SetResponse>>() {}))
        .isEqualTo(ApiResult.success(new SetResponse(true)));
  }

  private void assertStoredValue(String key, Object value) throws Exception {
    HttpResponse<String> response = post("/kv/get", Map.of("key", key));
    assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
    assertThat(mapper.readValue(response.body(), new TypeReference<ApiResult<GetResponse>>() {}))
        .isEqualTo(ApiResult.success(new GetResponse(key, value)));
  }

  private void assertError(HttpResponse<String> response, int status, ErrorCode code)
      throws Exception {
    assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
    assertThat(mapper.readValue(response.body(), new TypeReference<ApiResult<Void>>() {}))
        .isEqualTo(ApiResult.error(status, code.message()));
  }

  private HttpResponse<String> post(String path, Object body) throws Exception {
    return http.send(
        HttpRequest.newBuilder(uri(path))
            .timeout(HTTP_TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path) throws Exception {
    return http.send(
        HttpRequest.newBuilder(uri(path)).timeout(HTTP_TIMEOUT).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }
}
