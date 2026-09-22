package com.jam2in.arcus.http.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jam2in.arcus.http.btree.adapter.BTreeAdapter;
import com.jam2in.arcus.http.btree.dto.BKey;
import com.jam2in.arcus.http.btree.dto.BTreeGetRequest;
import com.jam2in.arcus.http.btree.service.BTreeService;
import com.jam2in.arcus.http.btree.validator.BTreeInputValidator;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.dto.CreateAttributes;
import com.jam2in.arcus.http.common.dto.OverflowAction;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.common.model.CollectionOperationResult;
import com.jam2in.arcus.http.common.validator.CollectionAttributesValidator;
import com.jam2in.arcus.http.list.adapter.ListAdapter;
import com.jam2in.arcus.http.list.dto.ListRangeRequest;
import com.jam2in.arcus.http.list.service.ListService;
import com.jam2in.arcus.http.map.adapter.MapAdapter;
import com.jam2in.arcus.http.map.dto.MapGetRequest;
import com.jam2in.arcus.http.map.service.MapService;
import com.jam2in.arcus.http.set.adapter.SetAdapter;
import com.jam2in.arcus.http.set.dto.SetCreateRequest;
import com.jam2in.arcus.http.set.dto.SetGetRequest;
import com.jam2in.arcus.http.set.service.SetService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CollectionServiceTest {

  private final CollectionAttributesValidator attributesValidator =
      new CollectionAttributesValidator();

  @Test
  @DisplayName("일반 조회에서 dropIfEmpty를 지정하면 Service가 Adapter 호출 전에 거부한다")
  void rejectsDropIfEmptyForReadOnlyOperation() {
    // given
    var adapter = mock(ListAdapter.class);
    var service = new ListService(adapter, attributesValidator);
    var request = new ListRangeRequest("key", 0, -1, true);

    // expected
    assertThatThrownBy(() -> service.get(request))
        .isInstanceOfSatisfying(ArcusHttpException.class,
            exception -> assertThat(exception.code()).isEqualTo(ErrorCode.INVALID_REQUEST));
    verifyNoInteractions(adapter);
  }

  @Test
  @DisplayName("조회 후 삭제는 별도 조회와 삭제로 분리하지 않는다")
  void preservesAtomicGetAndDelete() {
    // given
    var adapter = mock(ListAdapter.class);
    var service = new ListService(adapter, attributesValidator);
    var request = new ListRangeRequest("key", 0, -1, true);
    var operationResult = new CollectionOperationResult<>("DELETED_DROPPED", List.<Object>of("value"));
    when(adapter.getAndDelete("key", 0, -1, true)).thenReturn(operationResult);

    // when
    var result = service.getAndDelete(request);

    // then
    assertThat(result).isEqualTo(new CollectionResponse<>("DELETED_DROPPED", List.of("value")));
    verify(adapter).getAndDelete("key", 0, -1, true);
    verifyNoMoreInteractions(adapter);
  }

  @Test
  @DisplayName("Set 조회 후 삭제에서 생략한 dropIfEmpty는 false로 처리한다")
  void defaultsDropIfEmptyBeforeSetGetAndDelete() {
    // given
    var adapter = mock(SetAdapter.class);
    var service = new SetService(adapter, attributesValidator);
    var request = new SetGetRequest("key", 1, null);
    var operationResult = new CollectionOperationResult<>("DELETED", List.<Object>of("value"));
    when(adapter.getAndDelete("key", 1, false)).thenReturn(operationResult);

    // when
    var response = service.getAndDelete(request);

    // then
    assertThat(response).isEqualTo(new CollectionResponse<>("DELETED", List.of("value")));
    verify(adapter).getAndDelete("key", 1, false);
    verifyNoMoreInteractions(adapter);
  }

  @Test
  @DisplayName("Map 조회 선택자의 충돌은 Service가 Adapter 호출 전에 거부한다")
  void rejectsConflictingMapSelectors() {
    // given
    var adapter = mock(MapAdapter.class);
    var service = new MapService(adapter, attributesValidator);
    var request = new MapGetRequest("key", "field", List.of("other"), false);

    // expected
    assertThatThrownBy(() -> service.get(request)).isInstanceOf(ArcusHttpException.class);
    verifyNoInteractions(adapter);
  }

  @Test
  @DisplayName("컬렉션에 맞지 않는 생성 속성은 Service에서 거부한다")
  void rejectsUnsupportedOverflowBeforeCreate() {
    // given
    var adapter = mock(SetAdapter.class);
    var service = new SetService(adapter, attributesValidator);
    var attributes = new CreateAttributes(null, null, OverflowAction.head_trim, null, null, null);
    var request = new SetCreateRequest("key", attributes);

    // expected
    assertThatThrownBy(() -> service.create(request)).isInstanceOf(ArcusHttpException.class);
    verifyNoInteractions(adapter);
  }

  @Test
  @DisplayName("B+Tree 범위의 bKey 타입이 다르면 Service에서 거부한다")
  void rejectsMixedBKeyRangeBeforeRead() {
    // given
    var adapter = mock(BTreeAdapter.class);
    var service = new BTreeService(adapter, attributesValidator, new BTreeInputValidator());
    var request = new BTreeGetRequest(
        "key", new BKey(1L, null), new BKey(null, "02"), null, 0, 10, false);

    // expected
    assertThatThrownBy(() -> service.get(request)).isInstanceOf(ArcusHttpException.class);
    verifyNoInteractions(adapter);
  }

  @Test
  @DisplayName("B+Tree 단일 bKey 조회에 범위 옵션을 지정하면 Service에서 거부한다")
  void rejectsRangeOptionsForSingleBKey() {
    // given
    var adapter = mock(BTreeAdapter.class);
    var service = new BTreeService(adapter, attributesValidator, new BTreeInputValidator());
    var request = new BTreeGetRequest("key", new BKey(1L, null), null, null, 1, 0, false);

    // expected
    assertThatThrownBy(() -> service.get(request)).isInstanceOf(ArcusHttpException.class);
    verifyNoInteractions(adapter);
  }
}
