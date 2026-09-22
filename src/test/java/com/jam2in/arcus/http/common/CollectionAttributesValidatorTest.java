package com.jam2in.arcus.http.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.common.model.CollectionKind;
import com.jam2in.arcus.http.common.model.CollectionSettings;
import com.jam2in.arcus.http.common.model.OverflowPolicy;
import com.jam2in.arcus.http.common.validator.CollectionAttributesValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class CollectionAttributesValidatorTest {

  private final CollectionAttributesValidator validator = new CollectionAttributesValidator();

  @ParameterizedTest
  @CsvSource({
      "LIST,error", "LIST,head_trim", "LIST,tail_trim", "SET,error", "MAP,error",
      "BTREE,error", "BTREE,smallest_trim", "BTREE,largest_trim",
      "BTREE,smallest_silent_trim", "BTREE,largest_silent_trim"
  })
  @DisplayName("컬렉션 종류가 허용하는 overflow 동작을 허용한다")
  void acceptsSupportedOverflowPolicies(CollectionKind collectionType, OverflowPolicy action) {
    // given
    var request = new CollectionSettings(null, null, action, null, null, null);

    // expected
    assertThatCode(() -> validator.validateForCreate(request, collectionType))
        .doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource({
      "LIST,smallest_trim", "SET,head_trim", "SET,largest_trim", "MAP,tail_trim",
      "MAP,smallest_silent_trim", "BTREE,head_trim", "BTREE,tail_trim"
  })
  @DisplayName("컬렉션 종류에 맞지 않는 overflow 동작을 거부한다")
  void rejectsUnsupportedOverflowPolicies(CollectionKind collectionType, OverflowPolicy action) {
    // given
    var request = new CollectionSettings(null, null, action, null, null, null);

    // expected
    assertThatThrownBy(() -> validator.validateForCreate(request, collectionType))
        .isInstanceOfSatisfying(ArcusHttpException.class,
            exception -> assertThat(exception.code()).isEqualTo(ErrorCode.INVALID_REQUEST));
  }

  @ParameterizedTest
  @EnumSource(value = CollectionKind.class, names = {"LIST", "SET", "MAP"})
  @DisplayName("B+Tree 외의 컬렉션 생성에서는 두 표현의 bKey 범위를 거부한다")
  void rejectsBKeyRangesForOtherCollections(CollectionKind collectionType) {
    // given
    var numericRange = new CollectionSettings(null, null, null, null, 1L, null);
    var byteRange = new CollectionSettings(null, null, null, null, null, "01");

    // expected
    assertThatThrownBy(() -> validator.validateForCreate(numericRange, collectionType))
        .isInstanceOf(ArcusHttpException.class);
    assertThatThrownBy(() -> validator.validateForCreate(byteRange, collectionType))
        .isInstanceOf(ArcusHttpException.class);
  }

  @Test
  @DisplayName("생성은 빈 속성을 허용하지만 변경은 빈 속성을 거부한다")
  void distinguishesEmptyCreateAndUpdateAttributes() {
    // given
    var create = new CollectionSettings(null, null, null, null, null, null);
    var update = new CollectionSettings(null, null, null, null, null, null);

    // expected
    assertThatCode(() -> validator.validateForCreate(create, CollectionKind.LIST))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> validator.validateForUpdate(update)).isInstanceOf(ArcusHttpException.class);
    assertThatThrownBy(() -> validator.validateForUpdate(null)).isInstanceOf(ArcusHttpException.class);
  }

  @Test
  @DisplayName("생성과 변경 모두 숫자 범위와 바이트 범위의 동시 지정을 거부한다")
  void rejectsConflictingRangeRepresentations() {
    // given
    var create = new CollectionSettings(null, null, null, null, 1L, "01");
    var update = new CollectionSettings(null, null, null, null, 1L, "01");

    // expected
    assertThatThrownBy(() -> validator.validateForCreate(create, CollectionKind.BTREE))
        .isInstanceOf(ArcusHttpException.class);
    assertThatThrownBy(() -> validator.validateForUpdate(update)).isInstanceOf(ArcusHttpException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "0", "zz", "0x01", "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20"})
  @DisplayName("생성과 변경 모두 잘못된 바이트 범위를 거부한다")
  void rejectsInvalidByteRanges(String hex) {
    // given
    var create = new CollectionSettings(null, null, null, null, null, hex);
    var update = new CollectionSettings(null, null, null, null, null, hex);

    // expected
    assertThatThrownBy(() -> validator.validateForCreate(create, CollectionKind.BTREE))
        .isInstanceOf(ArcusHttpException.class);
    assertThatThrownBy(() -> validator.validateForUpdate(update)).isInstanceOf(ArcusHttpException.class);
  }
}
