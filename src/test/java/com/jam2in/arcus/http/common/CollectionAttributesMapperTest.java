package com.jam2in.arcus.http.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.jam2in.arcus.http.common.adapter.CollectionAttributesMapper;
import com.jam2in.arcus.http.common.model.CollectionSettings;
import com.jam2in.arcus.http.common.model.OverflowPolicy;
import net.spy.memcached.collection.CollectionOverflowAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CollectionAttributesMapperTest {

  private final CollectionAttributesMapper mapper = new CollectionAttributesMapper();

  @Test
  @DisplayName("생성 속성을 생략하면 서버 기본값을 유지한다")
  void preservesServerDefaultsForMissingCreateAttributes() {
    // given
    CollectionSettings request = null;

    // when
    var attributes = mapper.toCreateAttributes(request);

    // then
    assertThat(attributes.getExpireTime()).isNull();
    assertThat(attributes.getMaxCount()).isNull();
    assertThat(attributes.getReadable()).isNull();
    assertThat(attributes.getOverflowAction()).isNull();
  }

  @Test
  @DisplayName("생성 속성의 값과 B+Tree 범위를 변환한다")
  void mapsCreationFieldsAndNumericRange() {
    // given
    var request = new CollectionSettings(0, -1L, OverflowPolicy.smallest_trim, false, 0L, null);

    // when
    var attributes = mapper.toCreateAttributes(request);

    // then
    assertThat(attributes.getExpireTime()).isZero();
    assertThat(attributes.getMaxCount()).isEqualTo(-1);
    assertThat(attributes.getReadable()).isFalse();
    assertThat(attributes.getOverflowAction()).isEqualTo(CollectionOverflowAction.smallest_trim);
    assertThat(attributes.getMaxBkeyRange()).isZero();
  }

  @Test
  @DisplayName("속성 변경은 요청한 필드만 설정한다")
  void preservesUnspecifiedUpdateFields() {
    // given
    var request = new CollectionSettings(60, null, null, null, null, null);

    // when
    var attributes = mapper.toUpdateAttributes(request);

    // then
    assertThat(attributes.getExpireTime()).isEqualTo(60);
    assertThat(attributes.getMaxCount()).isNull();
    assertThat(attributes.getReadable()).isNull();
    assertThat(attributes.getOverflowAction()).isNull();
    assertThat(attributes.getMaxBkeyRange()).isNull();
  }

  @Test
  @DisplayName("변경의 0과 false를 생략으로 취급하지 않는다")
  void preservesZeroAndFalseUpdates() {
    // given
    var request = new CollectionSettings(0, 0L, OverflowPolicy.error, false, null, null);

    // when
    var attributes = mapper.toUpdateAttributes(request);

    // then
    assertThat(attributes.getExpireTime()).isZero();
    assertThat(attributes.getMaxCount()).isZero();
    assertThat(attributes.getReadable()).isFalse();
    assertThat(attributes.getOverflowAction()).isEqualTo(CollectionOverflowAction.error);
  }

  @Test
  @DisplayName("생성과 변경 모두 16진수 범위를 동일한 바이트로 변환한다")
  void mapsByteRanges() {
    // given
    var create = new CollectionSettings(null, null, null, null, null, "01Af");
    var update = new CollectionSettings(null, null, null, null, null, "01Af");

    // when
    var created = mapper.toCreateAttributes(create);
    var updated = mapper.toUpdateAttributes(update);

    // then
    assertThat(created.getMaxBkeyRangeByBytes()).containsExactly(1, (byte) 0xaf);
    assertThat(updated.getMaxBkeyRangeByBytes()).containsExactly(1, (byte) 0xaf);
  }
}
