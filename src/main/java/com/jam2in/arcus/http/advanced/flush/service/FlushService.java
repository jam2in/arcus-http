package com.jam2in.arcus.http.advanced.flush.service;

import com.jam2in.arcus.http.advanced.flush.adapter.FlushAdapter;
import com.jam2in.arcus.http.advanced.flush.dto.FlushRequest;
import com.jam2in.arcus.http.common.dto.BooleanResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FlushService {

  private final FlushAdapter adapter;

  public BooleanResponse flush(FlushRequest request) {
    var result = adapter.flush(request.prefix(), request.delay());
    return new BooleanResponse(result);
  }

}
