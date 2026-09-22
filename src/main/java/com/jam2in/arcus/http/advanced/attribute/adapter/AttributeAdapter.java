package com.jam2in.arcus.http.advanced.attribute.adapter;

import static com.jam2in.arcus.http.common.adapter.ClientSupport.hex;

import com.jam2in.arcus.http.advanced.attribute.model.AttributeBKey;
import com.jam2in.arcus.http.advanced.attribute.model.ItemAttributes;
import com.jam2in.arcus.http.common.adapter.ClientSupport;
import com.jam2in.arcus.http.common.adapter.CollectionAttributesMapper;
import com.jam2in.arcus.http.common.model.CollectionOperationResult;
import com.jam2in.arcus.http.common.model.CollectionSettings;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.ArcusClientPool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AttributeAdapter {

  private final ArcusClientPool client;
  private final ClientSupport support;
  private final CollectionAttributesMapper attributesMapper;

  public CollectionOperationResult<ItemAttributes> getAttributes(String key) {
    var result = support.collection(() -> client.asyncGetAttr(key));
    var attributes = result.value();

    return new CollectionOperationResult<>(result.status(), new ItemAttributes(
        attributes.getType() == null ? null : attributes.getType().toString(),
        attributes.getFlags(), attributes.getExpireTime(),
        attributes.getCount(), attributes.getMaxCount(), attributes.getReadable(),
        attributes.getOverflowAction() == null ? null : attributes.getOverflowAction().name(),
        toBKey(attributes::getMaxBkeyRange, attributes::getMaxBkeyRangeByBytes),
        toBKey(attributes::getMinBkey, attributes::getMinBkeyByBytes),
        toBKey(attributes::getMaxBkey, attributes::getMaxBkeyByBytes), attributes.getTrimmed()));
  }

  // The 1.16.2 attribute API throws for the getter of the other bKey type.
  private AttributeBKey toBKey(Supplier<Long> number, Supplier<byte[]> bytes) {
    try {
      Long value = number.get();
      return value == null ? null : new AttributeBKey(value, null);
    } catch (IllegalStateException byteKey) {
      return new AttributeBKey(null, hex(bytes.get()));
    }
  }

  public CollectionOperationResult<Boolean> setAttributes(
      String key, CollectionSettings attributes
  ) {
    var clientAttributes = attributesMapper.toUpdateAttributes(attributes);
    return support.collection(() -> client.asyncSetAttr(key, clientAttributes));
  }
}
