package com.jam2in.arcus.http.integration;

import static net.spy.memcached.transcoders.TranscoderUtils.SERIALIZED;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.spy.memcached.ArcusClientPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CollectionsIntegrationTest {

  @LocalServerPort
  private int port;
  @Autowired
  private JsonMapper mapper;
  @Autowired
  private ArcusClientPool client;
  private final HttpClient http = HttpClient.newHttpClient();
  private final String key = "arcus-http-integration:" + UUID.randomUUID();

  @AfterEach
  void cleanUp() throws Exception {
    try {
      client.delete(key).get();
    } finally {
      http.close();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"list", "set", "map", "btree"})
  @DisplayName("컬렉션 생성과 속성 변경에 30일 이후의 절대 만료 시각을 적용한다")
  void preservesAbsoluteExpiration(String type) throws Exception {
    // given
    long ttl = Duration.ofDays(31).toSeconds();
    long expireTime = Instant.now().getEpochSecond() + ttl;

    // expected
    request("POST", "/" + type + "/create",
        Map.of("key", key, "attributes", Map.of("expireTime", expireTime)));
    assertThat(client.asyncGetAttr(key).get().getExpireTime().longValue())
        .isBetween(ttl - 10, ttl + 1);
    long updatedTtl = Duration.ofDays(32).toSeconds();
    request("PATCH", "/item/set-attr", Map.of("key", key,
        "attributes", Map.of("expireTime", Instant.now().getEpochSecond() + updatedTtl)));
    assertThat(client.asyncGetAttr(key).get().getExpireTime().longValue())
        .isBetween(updatedTtl - 10, updatedTtl + 1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"list", "set", "map", "btree"})
  @DisplayName("실제 Arcus에서 컬렉션 생성·삽입·조회·속성 변경·삭제를 확인한다")
  void roundTripsCollections(String type) throws Exception {
    // given
    Map<String, Object> insert = switch (type) {
      case "list" -> Map.of("key", key, "index", 0, "value", "안녕하세요");
      case "set" -> Map.of("key", key, "value", "안녕하세요");
      case "map" -> Map.of("key", key, "mKey", "field", "value", "안녕하세요");
      default -> Map.of("key", key, "bKey", Map.of("number", 1), "eFlag", "01", "value", "안녕하세요");
    };
    Map<String, Object> query = switch (type) {
      case "list" -> Map.of("key", key, "from", 0);
      case "set" -> Map.of("key", key, "count", 0);
      case "map" -> Map.of("key", key, "mKey", "field");
      default -> Map.of("key", key, "from", Map.of("number", 1));
    };
    Map<String, Object> delete = type.equals("set") ? Map.of("key", key, "value", "안녕하세요") : query;

    // expected
    request("POST", "/" + type + "/create", Map.of("key", key, "attributes", Map.of("expireTime", 60)));
    request("POST", "/" + type + "/insert", insert);
    var result = request("POST", "/" + type + "/get", query);
    var value = result.path("content").path("value");
    String stored = switch (type) {
      case "map" -> value.path("field").asString();
      case "btree" -> value.path(0).path("value").asString();
      default -> value.path(0).asString();
    };
    assertThat(stored).isEqualTo("안녕하세요");
    request("PATCH", "/item/set-attr", Map.of("key", key, "attributes", Map.of("expireTime", 120)));
    assertThat(request("POST", "/item/get-attr", Map.of("key", key)).path("content").path("value").path("count").asLong()).isEqualTo(1);
    request("DELETE", "/" + type + "/delete", delete);
  }

  @ParameterizedTest
  @ValueSource(strings = {"list", "set", "map", "btree"})
  @DisplayName("실제 Arcus의 빈 컬렉션에 Java 직렬화 flags를 설정한다")
  void createsEmptyCollectionWithSerializedFlags(String collectionType) throws Exception {
    // given
    var body = Map.of("key", key, "attributes", Map.of("expireTime", 60));

    // when
    request("POST", "/" + collectionType + "/create", body);
    var attributes = request("POST", "/item/get-attr", Map.of("key", key))
        .path("content").path("value");

    // then
    assertThat(attributes.path("flags").asInt()).isEqualTo(SERIALIZED);
    assertThat(attributes.path("count").asLong()).isZero();
  }

  @ParameterizedTest
  @MethodSource("mixedCollectionTypes")
  @DisplayName("명시적 생성과 자동 생성 모두 혼합 JSON 값을 같은 flags로 저장한다")
  void roundTripsMixedValues(String type, boolean autoCreate) throws Exception {
    // given
    var values = mixedValues();
    var query = collectionQuery(type);

    // expected
    if (!autoCreate) {
      request("POST", "/" + type + "/create",
          Map.of("key", key, "attributes", Map.of("expireTime", 60)));
    }
    for (int index = 0; index < values.size(); index++) {
      var insert = insertion(type, index, values.get(index));
      if (autoCreate) {
        insert.put("attributesForCreate", Map.of("expireTime", 60));
      }
      request("POST", "/" + type + "/insert", insert);
    }
    var attributes = request("POST", "/item/get-attr", Map.of("key", key));
    assertThat(attributes.at("/content/value/flags").asInt()).isEqualTo(SERIALIZED);
    var found = request("POST", "/" + type + "/get", query).at("/content/value");
    assertValues(type, found, values);
    var deleted = request("PATCH", "/" + type + "/get-and-delete", query)
        .at("/content/value");
    assertValues(type, deleted, values);
    assertThat(request("POST", "/item/get-attr", Map.of("key", key))
        .at("/content/value/count").asInt()).isZero();
  }

  static Stream<Arguments> mixedCollectionTypes() {
    return Stream.of("list", "set", "map", "btree").flatMap(type ->
        Stream.of(Arguments.of(type, false), Arguments.of(type, true)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"list", "set", "map", "btree"})
  @DisplayName("bulk와 piped 삽입도 객체와 배열 및 원시 값을 보존한다")
  void storesObjectsThroughBulkAndPipedInsert(String type) throws Exception {
    // given
    var values = mixedValues().subList(0, 3);
    var bulk = insertion(type, 0, values.getFirst());
    bulk.remove("key");
    bulk.put("keys", List.of(key));
    bulk.put("attributesForCreate", Map.of("expireTime", 60));
    var piped = new LinkedHashMap<String, Object>();
    piped.put("key", key);
    switch (type) {
      case "list" -> {
        piped.put("index", 1);
        piped.put("values", values.subList(1, 3));
      }
      case "set" -> piped.put("values", values.subList(1, 3));
      case "map" -> piped.put("elements", Map.of("1", values.get(1), "2", values.get(2)));
      default -> piped.put("elements", List.of(
          Map.of("bKey", Map.of("number", 1), "value", values.get(1)),
          Map.of("bKey", Map.of("number", 2), "value", values.get(2))));
    }

    // expected
    assertThat(request("POST", "/" + type + "/insert-bulk", bulk)
        .at("/content/failures").isEmpty()).isTrue();
    assertThat(request("POST", "/" + type + "/piped-insert-bulk", piped)
        .at("/content/complete").asBoolean()).isTrue();
    assertValues(type, request("POST", "/" + type + "/get", collectionQuery(type))
        .at("/content/value"), values);
  }

  @ParameterizedTest
  @ValueSource(strings = {"map", "btree"})
  @DisplayName("단일 갱신과 piped 갱신에서 객체와 배열을 읽을 수 있다")
  void updatesObjects(String type) throws Exception {
    // given
    var insert = insertion(type, 0, "before");
    insert.put("attributesForCreate", Map.of("expireTime", 60));
    var object = Map.of("name", "after");
    var array = List.of(1, "1", true);
    var update = insertion(type, 0, object);
    var piped = type.equals("map")
        ? Map.of("key", key, "elements", Map.of("0", array))
        : Map.of("key", key, "elements",
            List.of(Map.of("bKey", Map.of("number", 0), "value", array)));

    // expected
    request("POST", "/" + type + "/insert", insert);
    request("PATCH", "/" + type + "/update", update);
    assertValues(type, request("POST", "/" + type + "/get", collectionQuery(type))
        .at("/content/value"), List.of(object));
    request("PATCH", "/" + type + "/piped-update-bulk", piped);
    assertValues(type, request("POST", "/" + type + "/get", collectionQuery(type))
        .at("/content/value"), List.of(array));
  }

  @Test
  @DisplayName("Set의 숫자와 문자열 및 객체와 배열의 존재 여부를 원래 값으로 응답한다")
  void distinguishesSetValuesInExistenceResults() throws Exception {
    // given
    var object = Map.of("name", "member");
    var array = List.of(1, "1");
    var stored = List.of(1, object, array);
    var queried = List.of(1, "1", object, array);

    // expected
    request("POST", "/set/piped-insert-bulk", Map.of("key", key, "values", stored,
        "attributesForCreate", Map.of("expireTime", 60)));
    var found = request("POST", "/set/piped-exist-bulk",
        Map.of("key", key, "values", queried)).at("/content/value");
    assertThat(found).isEqualTo(mapper.valueToTree(List.of(
        Map.of("value", 1, "exists", true),
        Map.of("value", "1", "exists", false),
        Map.of("value", object, "exists", true),
        Map.of("value", array, "exists", true))));
    assertThat(request("POST", "/set/exist", Map.of("key", key, "value", object))
        .at("/content/value").asBoolean()).isTrue();
    request("DELETE", "/set/delete", Map.of("key", key, "value", object));
    assertThat(request("POST", "/set/exist", Map.of("key", key, "value", object))
        .at("/content/value").asBoolean()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"incr", "decr"})
  @DisplayName("직렬화된 B+Tree 값의 증감은 서버 오류를 공통 오류 응답으로 반환한다")
  void returnsServerCounterFailure(String operation) throws Exception {
    // given
    request("POST", "/btree/insert", Map.of("key", key, "bKey", Map.of("number", 1),
        "value", 10, "attributesForCreate", Map.of("expireTime", 60)));
    var body = Map.of("key", key, "bKey", Map.of("number", 1), "by", 1);
    var counter = HttpRequest.newBuilder(URI.create("http://localhost:" + port
            + "/btree/" + operation))
        .timeout(Duration.ofSeconds(5))
        .header("Content-Type", "application/json")
        .method("PATCH", HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
        .build();

    // when
    var response = http.send(counter, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(response.statusCode()).as(response.body()).isEqualTo(502);
    assertThat(mapper.readTree(response.body()).path("code").asInt()).isEqualTo(502);
    assertThat(request("POST", "/btree/get", Map.of("key", key, "from", Map.of("number", 1)))
        .at("/content/value/0/value").asInt()).isEqualTo(10);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  @DisplayName("B+Tree의 bulk와 smget 및 위치와 trim 조회에서도 객체를 보존한다")
  void preservesObjectsInAdvancedBtreeReads(boolean binaryKey) throws Exception {
    // given
    var firstBKey = binaryKey ? Map.of("hex", "01") : Map.of("number", 1);
    var secondBKey = binaryKey ? Map.of("hex", "02") : Map.of("number", 2);
    var thirdBKey = binaryKey ? Map.of("hex", "03") : Map.of("number", 3);
    var object = Map.of("name", "first");
    var array = List.of(1, "1");
    var bulkQuery = Map.of("keys", List.of(key), "from", firstBKey,
        "to", secondBKey, "count", 10);

    // expected
    request("POST", "/btree/create", Map.of("key", key, "attributes",
        Map.of("expireTime", 60, "maxCount", 2, "overflowAction", "smallest_trim")));
    request("POST", "/btree/insert", Map.of("key", key, "bKey", firstBKey, "value", object));
    request("POST", "/btree/insert", Map.of("key", key, "bKey", secondBKey, "value", array));
    var bulk = request("POST", "/btree/get-bulk", bulkQuery)
        .path("content").path(key).path("value");
    assertValues("btree", bulk, List.of(object, array));
    var merged = request("POST", "/btree/smget", bulkQuery).at("/content/value/elements");
    assertThat(merged.path(0).at("/element/value")).isEqualTo(mapper.valueToTree(object));
    assertThat(merged.path(1).at("/element/value")).isEqualTo(mapper.valueToTree(array));
    var positioned = request("POST", "/btree/get-by-position",
        Map.of("key", key, "order", "ASC", "from", 0, "to", 1));
    assertThat(positioned.at("/content/value/0/value")).isEqualTo(mapper.valueToTree(object));
    assertThat(positioned.at("/content/value/1/value")).isEqualTo(mapper.valueToTree(array));
    var around = request("POST", "/btree/find-position-with-get",
        Map.of("key", key, "order", "ASC", "bKey", secondBKey, "count", 1));
    assertThat(around.at("/content/value/1/value")).isEqualTo(mapper.valueToTree(array));
    var trimmed = request("POST", "/btree/insert-and-get-trimmed",
        Map.of("key", key, "bKey", thirdBKey, "value", true));
    assertThat(trimmed.at("/content/value/trimmed/value")).isEqualTo(mapper.valueToTree(object));
  }

  private List<Object> mixedValues() {
    return mapper.readValue("""
        [{"name":"사용자","items":[1,true,null]},[1,"1",null],true,
         "1",1,2147483648,1.25,{},[]]
        """, mapper.getTypeFactory().constructCollectionType(List.class, Object.class));
  }

  private Map<String, Object> insertion(String type, int index, Object value) {
    var body = new LinkedHashMap<String, Object>();
    body.put("key", key);
    body.put("value", value);
    switch (type) {
      case "list" -> body.put("index", index);
      case "map" -> body.put("mKey", Integer.toString(index));
      case "btree" -> body.put("bKey", Map.of("number", index));
      default -> {
      }
    }
    return body;
  }

  private Map<String, Object> collectionQuery(String type) {
    return switch (type) {
      case "list" -> Map.of("key", key, "from", 0, "to", -1);
      case "set" -> Map.of("key", key, "count", 0);
      case "map" -> Map.of("key", key);
      default -> Map.of("key", key, "from", Map.of("number", 0),
          "to", Map.of("number", 100));
    };
  }

  private void assertValues(String type, JsonNode found, List<?> expected) {
    var actual = new ArrayList<JsonNode>();
    for (int index = 0; index < found.size(); index++) {
      actual.add(switch (type) {
        case "map" -> found.path(Integer.toString(index));
        case "btree" -> found.path(index).path("value");
        default -> found.path(index);
      });
    }
    List<JsonNode> expectedNodes = expected.stream().map(mapper::<JsonNode>valueToTree).toList();
    if (type.equals("set")) {
      assertThat(actual).containsExactlyInAnyOrderElementsOf(expectedNodes);
    } else {
      assertThat(actual).containsExactlyElementsOf(expectedNodes);
    }
  }

  @Test
  @DisplayName("실제 클라이언트가 잘못된 Map 원소 키를 거부한다")
  void rejectsInvalidMapKeyThroughClient() throws Exception {
    // given
    request("POST", "/map/create", Map.of("key", key, "attributes", Map.of("expireTime", 60)));
    var body = Map.of("key", key, "mKey", "가".repeat(84), "value", "v");
    var insert = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/map/insert"))
        .timeout(Duration.ofSeconds(5))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
        .build();

    // when
    var response = http.send(insert, HttpResponse.BodyHandlers.ofString());
    var attributes = request("POST", "/item/get-attr", Map.of("key", key))
        .path("content").path("value");

    // then
    assertThat(response.statusCode()).isEqualTo(400);
    assertThat(mapper.readTree(response.body()).path("code").asInt()).isEqualTo(400);
    assertThat(attributes.path("count").asLong()).isZero();
  }

  private JsonNode request(String method, String path, Object body) throws Exception {
    var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
        .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
    var response = http.send(request, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).as(path + ": " + response.body()).isEqualTo(200);
    var json = mapper.readTree(response.body());
    assertThat(json.path("code").asInt()).isZero();
    return json;
  }
}
