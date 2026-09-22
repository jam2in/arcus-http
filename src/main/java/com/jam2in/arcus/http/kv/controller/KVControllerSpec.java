package com.jam2in.arcus.http.kv.controller;

import com.jam2in.arcus.http.common.dto.BooleanResponse;
import com.jam2in.arcus.http.common.dto.BulkResponse;
import com.jam2in.arcus.http.common.response.ApiResult;
import com.jam2in.arcus.http.kv.dto.request.AffixRequest;
import com.jam2in.arcus.http.kv.dto.request.CounterRequest;
import com.jam2in.arcus.http.kv.dto.request.DeleteBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.GetBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.GetRequest;
import com.jam2in.arcus.http.kv.dto.request.SetRequest;
import com.jam2in.arcus.http.kv.dto.request.StoreBulkRequest;
import com.jam2in.arcus.http.kv.dto.request.TouchRequest;
import com.jam2in.arcus.http.kv.dto.response.CounterResponse;
import com.jam2in.arcus.http.kv.dto.response.GetBulkResponse;
import com.jam2in.arcus.http.kv.dto.response.GetResponse;
import com.jam2in.arcus.http.kv.dto.response.GetsBulkResponse;
import com.jam2in.arcus.http.kv.dto.response.GetsResponse;
import com.jam2in.arcus.http.kv.dto.response.SetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "KV", description = "Key-Value 데이터 관리")
@ApiResponses({
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(schema = @Schema(implementation = ApiResult.class))),
    @ApiResponse(
        responseCode = "502",
        description = "Arcus 연산 실패",
        content = @Content(schema = @Schema(implementation = ApiResult.class))),
    @ApiResponse(
        responseCode = "503",
        description = "Arcus 연결 불가",
        content = @Content(schema = @Schema(implementation = ApiResult.class))),
    @ApiResponse(
        responseCode = "504",
        description = "타임아웃. 저장 결과가 불확실할 수 있음",
        content = @Content(schema = @Schema(implementation = ApiResult.class)))
})
public interface KVControllerSpec {

  @Operation(
      operationId = "kv_set",
      summary = "Key-Value 저장",
      description = "키에 값을 저장하며 기존 값이 있으면 덮어씁니다. expireTime의 상대 TTL, Unix timestamp와 특수값은 요청 스키마를 참고하세요.")
  @ApiResponse(
      responseCode = "200",
      description = "저장 완료",
      useReturnTypeSchema = true)
  ApiResult<SetResponse> set(SetRequest request);

  @Operation(operationId = "kv_get", summary = "Key-Value 조회", description = "키에 저장된 값을 조회합니다.")
  @ApiResponse(
      responseCode = "200",
      description = "조회 완료",
      useReturnTypeSchema = true)
  @ApiResponse(
      responseCode = "404",
      description = "키 미존재 또는 만료",
      content = @Content(schema = @Schema(implementation = ApiResult.class)))
  ApiResult<GetResponse> get(GetRequest request);

  @Operation(operationId = "kv_add", summary = "키가 없을 때 저장")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<SetResponse> add(SetRequest request);

  @Operation(operationId = "kv_replace", summary = "기존 값 교체")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<SetResponse> replace(SetRequest request);

  @Operation(operationId = "kv_append", summary = "기존 값 뒤에 추가")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<SetResponse> append(AffixRequest request);

  @Operation(operationId = "kv_prepend", summary = "기존 값 앞에 추가")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<SetResponse> prepend(AffixRequest request);

  @Operation(operationId = "kv_storeBulk", summary = "여러 키 저장, 키별 실패 반환")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<BulkResponse> storeBulk(StoreBulkRequest request);

  @Operation(operationId = "kv_getBulk", summary = "여러 키 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<GetBulkResponse> getBulk(GetBulkRequest request);

  @Operation(operationId = "kv_gets", summary = "값과 CAS 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<GetsResponse> gets(GetRequest request);

  @Operation(operationId = "kv_getsBulk", summary = "여러 값과 CAS 조회")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<GetsBulkResponse> getsBulk(GetBulkRequest request);

  @Operation(operationId = "kv_incr", summary = "숫자 값 증가")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CounterResponse> incr(CounterRequest request);

  @Operation(operationId = "kv_decr", summary = "숫자 값 감소")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<CounterResponse> decr(CounterRequest request);

  @Operation(operationId = "kv_delete", summary = "아이템 삭제")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<BooleanResponse> delete(GetRequest request);

  @Operation(operationId = "kv_touch", summary = "만료시간 갱신")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<BooleanResponse> touch(TouchRequest request);

  @Operation(operationId = "kv_getAndTouch", summary = "조회와 만료시간 갱신")
  @ApiResponse(responseCode = "200", description = "연산 결과", useReturnTypeSchema = true)
  ApiResult<GetResponse> getAndTouch(TouchRequest request);

  @Operation(operationId = "kv_deleteBulk", summary = "delete-bulk")
  @ApiResponse(responseCode = "200", description = "일괄 처리 결과", useReturnTypeSchema = true)
  ApiResult<BulkResponse> deleteBulk(DeleteBulkRequest request);

}
