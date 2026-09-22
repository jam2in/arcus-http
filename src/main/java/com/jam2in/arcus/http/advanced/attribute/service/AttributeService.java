package com.jam2in.arcus.http.advanced.attribute.service;

import com.jam2in.arcus.http.advanced.attribute.adapter.AttributeAdapter;
import com.jam2in.arcus.http.advanced.attribute.dto.AttributesResponse;
import com.jam2in.arcus.http.advanced.attribute.dto.GetAttributesRequest;
import com.jam2in.arcus.http.advanced.attribute.dto.SetAttributesRequest;
import com.jam2in.arcus.http.common.dto.CollectionResponse;
import com.jam2in.arcus.http.common.validator.CollectionAttributesValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttributeService {

  private final AttributeAdapter adapter;
  private final CollectionAttributesValidator attributesValidator;

  public CollectionResponse<AttributesResponse> getAttributes(GetAttributesRequest request) {
    var result = adapter.getAttributes(request.key());
    return CollectionResponse.from(result, AttributesResponse::from);
  }

  public CollectionResponse<Boolean> setAttributes(SetAttributesRequest request) {
    var attributes = request.attributes() == null ? null : request.attributes().toSettings();
    attributesValidator.validateForUpdate(attributes);

    var result = adapter.setAttributes(request.key(), attributes);
    return CollectionResponse.from(result);
  }

}
