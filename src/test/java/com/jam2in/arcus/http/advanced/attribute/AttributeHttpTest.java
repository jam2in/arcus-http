package com.jam2in.arcus.http.advanced.attribute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jam2in.arcus.http.support.ClientHttpTest;
import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.CollectionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AttributeHttpTest extends ClientHttpTest {

  @Test
  @DisplayName("속성의 숫자·바이트 bKey 메타데이터를 안전하게 변환한다")
  void mapsBothAttributeKeyTypes() throws Exception {
    // given
    var numeric = new CollectionAttributes();
    numeric.setMaxBkeyRange(10L);
    var bytes = new CollectionAttributes();
    bytes.setMaxBkeyRangeByBytes(new byte[]{1, 2});
    var first = collection(numeric, CollectionResponse.END);
    var second = collection(bytes, CollectionResponse.END);
    when(client.asyncGetAttr("number")).thenReturn(first);
    when(client.asyncGetAttr("bytes")).thenReturn(second);

    // expected
    mvc.perform(post("/item/get-attr")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"number\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value.maxBKeyRange.number").value(10));
    mvc.perform(post("/item/get-attr")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"bytes\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.value.maxBKeyRange.hex").value("0102"));
  }

  @Test
  @DisplayName("속성 변경은 PATCH로 요청한 필드만 전달한다")
  void updatesRequestedAttributes() throws Exception {
    // given
    var updated = collection(true, CollectionResponse.OK);
    when(client.asyncSetAttr(eq("k"), any())).thenReturn(updated);

    // expected
    mvc.perform(patch("/item/set-attr")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"attributes\":{\"expireTime\":60,\"overflowAction\":\"head_trim\"}}"))
        .andExpect(status().isOk());
    verify(client).asyncSetAttr(eq("k"), argThat(attributes -> attributes.getExpireTime() == 60
        && ((CollectionAttributes) attributes).getOverflowAction() == CollectionOverflowAction.head_trim
        && ((CollectionAttributes) attributes).getReadable() == null));
  }

  @Test
  @DisplayName("빈 속성 변경과 읽기 전용 속성 변경을 거부한다")
  void rejectsEmptyOrReadonlyChanges() throws Exception {
    // given
    var attributes = new String[]{
        "{}",
        "{\"count\":1}",
        "{\"maxBKeyRange\":1,\"maxBKeyRangeHex\":\"01\"}",
        "{\"expireTime\":-2}",
        "{\"expireTime\":2147483648}",
        "{\"maxCount\":-2}",
        "{\"maxBKeyRange\":-1}",
        "{\"maxBKeyRangeHex\":\"zz\"}"
    };

    // expected
    for (String attributesJson : attributes) {
      mvc.perform(patch("/item/set-attr")
              .contentType(MediaType.APPLICATION_JSON)
          .content("{\"key\":\"k\",\"attributes\":" + attributesJson + "}"))
          .andExpect(status().isBadRequest());
    }
    verifyNoInteractions(client);
  }

  @Test
  @DisplayName("아이템 미존재와 속성 값 오류를 HTTP 상태로 변환한다")
  void mapsAttributeErrors() throws Exception {
    // given
    var missing = collection((CollectionAttributes) null, CollectionResponse.NOT_FOUND);
    var invalid = collection(false, CollectionResponse.ATTR_ERROR_BAD_VALUE);
    when(client.asyncGetAttr("k")).thenReturn(missing);
    when(client.asyncSetAttr(eq("k"), any())).thenReturn(invalid);

    // expected
    mvc.perform(post("/item/get-attr")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"key\":\"k\"}"))
        .andExpect(status().isNotFound());
    mvc.perform(patch("/item/set-attr")
            .contentType(MediaType.APPLICATION_JSON)
        .content("{\"key\":\"k\",\"attributes\":{\"readable\":false}}"))
        .andExpect(status().isBadRequest());
  }
}
