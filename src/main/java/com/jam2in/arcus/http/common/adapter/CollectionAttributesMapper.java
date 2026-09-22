package com.jam2in.arcus.http.common.adapter;

import com.jam2in.arcus.http.common.model.CollectionSettings;
import com.jam2in.arcus.http.common.model.OverflowPolicy;
import java.util.HexFormat;
import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import org.springframework.stereotype.Component;

@Component
public class CollectionAttributesMapper {

  public CollectionAttributes toCreateAttributes(
      CollectionSettings request
  ) {
    if (request == null) {
      return new CollectionAttributes();
    }

    var attributes = new CollectionAttributes(
        request.expireTime(), request.maxCount(), toOverflowAction(request.overflowAction()));
    attributes.setReadable(request.readable());
    setBKeyRange(attributes, request.maxBKeyRange(), request.maxBKeyRangeHex());
    return attributes;
  }

  public CollectionAttributes toUpdateAttributes(CollectionSettings request) {
    var attributes = new CollectionAttributes(
        request.expireTime(), request.maxCount(), toOverflowAction(request.overflowAction()));
    attributes.setReadable(request.readable());
    setBKeyRange(attributes, request.maxBKeyRange(), request.maxBKeyRangeHex());
    return attributes;
  }

  private CollectionOverflowAction toOverflowAction(OverflowPolicy overflowAction) {
    return overflowAction == null ? null : CollectionOverflowAction.valueOf(overflowAction.name());
  }

  private void setBKeyRange(CollectionAttributes attributes, Long number, String hex) {
    if (number != null) {
      attributes.setMaxBkeyRange(number);
    } else if (hex != null) {
      attributes.setMaxBkeyRangeByBytes(HexFormat.of().parseHex(hex));
    }
  }
}
