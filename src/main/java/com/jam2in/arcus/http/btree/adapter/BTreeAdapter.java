package com.jam2in.arcus.http.btree.adapter;

import static com.jam2in.arcus.http.btree.adapter.BTreeParameterMapper.defaultZero;
import static com.jam2in.arcus.http.btree.adapter.BTreeParameterMapper.isEnabled;
import static com.jam2in.arcus.http.btree.adapter.BTreeParameterMapper.toClientOrder;
import static com.jam2in.arcus.http.common.adapter.ClientSupport.hex;

import com.jam2in.arcus.http.btree.model.BTreeElement;
import com.jam2in.arcus.http.btree.model.BTreeElementUpdate;
import com.jam2in.arcus.http.btree.model.BTreeEntry;
import com.jam2in.arcus.http.btree.model.BTreeKey;
import com.jam2in.arcus.http.btree.model.BTreeStoreResult;
import com.jam2in.arcus.http.btree.model.FlagFilter;
import com.jam2in.arcus.http.btree.model.FlagUpdate;
import com.jam2in.arcus.http.btree.model.SortMergeResult;
import com.jam2in.arcus.http.btree.model.SortOrder;
import com.jam2in.arcus.http.btree.model.TrimmedKey;
import com.jam2in.arcus.http.common.adapter.ClientSupport;
import com.jam2in.arcus.http.common.adapter.CollectionAttributesMapper;
import com.jam2in.arcus.http.common.model.BulkOperationResult;
import com.jam2in.arcus.http.common.model.CollectionOperationResult;
import com.jam2in.arcus.http.common.model.CollectionSettings;
import com.jam2in.arcus.http.common.model.PipedOperationResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.ArcusClientPool;
import net.spy.memcached.collection.BTreeGetResult;
import net.spy.memcached.collection.ByteArrayBKey;
import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.Element;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.internal.BTreeStoreAndGetFuture;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BTreeAdapter {

  private final ArcusClientPool client;
  private final ClientSupport support;
  private final CollectionAttributesMapper attributesMapper;

  public CollectionOperationResult<Boolean> create(
      String key,
      CollectionSettings attributes
  ) {
    return support.collection(() -> client.asyncBopCreate(key,
        ElementValueType.OTHERS,
        attributesMapper.toCreateAttributes(attributes)));
  }

  private CollectionAttributes toCreateAttributes(CollectionSettings attributes) {
    return attributes == null ? null
        : attributesMapper.toCreateAttributes(attributes);
  }

  private BTreeElement toElement(Element<Object> element) {
    if (element == null) {
      return null;
    }
    BTreeKey key = !element.getStringBkey().startsWith("0x")
        ? new BTreeKey(element.getLongBkey(), null)
        : new BTreeKey(null, hex(element.getByteArrayBkey()));
    return new BTreeElement(key, hex(element.getEFlag()),
        element.getValue());
  }

  public CollectionOperationResult<Boolean> insert(
      String key, BTreeKey bKey, String eFlag, Object value, CollectionSettings attributesForCreate
  ) {
    byte[] flag = hex(eFlag, 31);
    var attributes = toCreateAttributes(attributesForCreate);
    return support.collection(() -> bKey.number() != null
        ? client.asyncBopInsert(key, bKey.number(), flag, value,
        attributes)
        : client.asyncBopInsert(key, hex(bKey.hex(), 31), flag,
            value, attributes));
  }

  public CollectionOperationResult<Boolean> upsert(
      String key, BTreeKey bKey, String eFlag, Object value, CollectionSettings attributesForCreate
  ) {
    byte[] flag = hex(eFlag, 31);
    var attributes = toCreateAttributes(attributesForCreate);
    return support.collection(() -> bKey.number() != null
        ? client.asyncBopUpsert(key, bKey.number(), flag, value,
        attributes)
        : client.asyncBopUpsert(key, hex(bKey.hex(), 31), flag,
            value, attributes));
  }

  public CollectionOperationResult<BTreeStoreResult> insertAndGetTrimmed(
      String key, BTreeKey bKey, String eFlag, Object value, CollectionSettings attributesForCreate
  ) {
    byte[] flag = hex(eFlag, 31);
    var attributes = toCreateAttributes(attributesForCreate);
    return trimmed(() -> bKey.number() != null
        ? client.asyncBopInsertAndGetTrimmed(key, bKey.number(), flag,
        value, attributes)
        : client.asyncBopInsertAndGetTrimmed(key, hex(bKey.hex(), 31), flag,
            value,
            attributes));
  }

  public CollectionOperationResult<BTreeStoreResult> upsertAndGetTrimmed(
      String key, BTreeKey bKey, String eFlag, Object value, CollectionSettings attributesForCreate
  ) {
    byte[] flag = hex(eFlag, 31);
    var attributes = toCreateAttributes(attributesForCreate);
    return trimmed(() -> bKey.number() != null
        ? client.asyncBopUpsertAndGetTrimmed(key, bKey.number(), flag,
        value, attributes)
        : client.asyncBopUpsertAndGetTrimmed(key, hex(bKey.hex(), 31), flag,
            value,
            attributes));
  }

  private CollectionOperationResult<BTreeStoreResult> trimmed(
      Supplier<BTreeStoreAndGetFuture<Boolean, Object>> supplier) {
    return support.result(() -> {
      var future = supplier.get();
      var stored = support.collection(() -> future);
      return new CollectionOperationResult<>(stored.status(),
          new BTreeStoreResult(stored.value(), toElement(future.getElement())));
    });
  }

  public BulkOperationResult insertBulk(
      List<String> keys,
      BTreeKey bKey,
      String eFlag,
      Object value,
      CollectionSettings attributesForCreate
  ) {
    var flag = hex(eFlag, 31);
    var attributes = toCreateAttributes(attributesForCreate);
    return support.bulk(support.call(() -> bKey.number() != null
        ? client.asyncBopInsertBulk(keys, bKey.number(), flag, value,
        attributes)
        : client.asyncBopInsertBulk(keys, hex(bKey.hex(), 31), flag,
            value, attributes)));
  }

  public CollectionOperationResult<Boolean> update(
      String key, BTreeKey bKey, Object value, FlagUpdate eFlagUpdate
  ) {
    var flag = BTreeParameterMapper.toUpdate(eFlagUpdate);
    return support.collection(() -> bKey.number() != null
        ? client.asyncBopUpdate(key, bKey.number(), flag, value)
        : client.asyncBopUpdate(key, hex(bKey.hex(), 31), flag,
            value));
  }

  public CollectionOperationResult<Long> incr(
      String key, BTreeKey bKey, Integer by, Long initial, String eFlag
  ) {
    byte[] flag = hex(eFlag, 31);
    return support.collection(() -> {
      if (bKey.number() != null) {
        return initial == null ? client.asyncBopIncr(key, bKey.number(), by)
            : client.asyncBopIncr(key, bKey.number(), by, initial, flag);
      }
      byte[] byteKey = hex(bKey.hex(), 31);
      return initial == null ? client.asyncBopIncr(key, byteKey, by)
          : client.asyncBopIncr(key, byteKey, by, initial, flag);
    });
  }

  public CollectionOperationResult<Long> decr(
      String key, BTreeKey bKey, Integer by, Long initial, String eFlag
  ) {
    byte[] flag = hex(eFlag, 31);
    return support.collection(() -> {
      if (bKey.number() != null) {
        return initial == null ? client.asyncBopDecr(key, bKey.number(), by)
            : client.asyncBopDecr(key, bKey.number(), by, initial, flag);
      }
      byte[] byteKey = hex(bKey.hex(), 31);
      return initial == null ? client.asyncBopDecr(key, byteKey, by)
          : client.asyncBopDecr(key, byteKey, by, initial, flag);
    });
  }

  public CollectionOperationResult<List<BTreeElement>> get(
      String key,
      BTreeKey from,
      BTreeKey to,
      FlagFilter filter,
      Integer offset,
      Integer count,
      Boolean dropIfEmpty
  ) {
    return read(key, from, to, filter, offset, count, dropIfEmpty, false);
  }

  public CollectionOperationResult<List<BTreeElement>> getAndDelete(
      String key,
      BTreeKey from,
      BTreeKey to,
      FlagFilter filter,
      Integer offset,
      Integer count,
      Boolean dropIfEmpty
  ) {
    return read(key, from, to, filter, offset, count, dropIfEmpty, true);
  }

  private CollectionOperationResult<List<BTreeElement>> read(
      String key, BTreeKey from, BTreeKey to,
      FlagFilter filter, Integer offset, Integer count,
      Boolean dropIfEmpty, boolean delete
  ) {
    var clientFilter = BTreeParameterMapper.toFilter(filter);
    CollectionOperationResult<? extends Map<?, Element<Object>>> found;
    if (from.number() != null) {
      found = support.collection(() -> to == null
          ? client.asyncBopGet(key, from.number(), clientFilter, delete,
          isEnabled(dropIfEmpty))
          : client.asyncBopGet(key, from.number(), to.number(),
              clientFilter,
              defaultZero(offset), defaultZero(count), delete, isEnabled(dropIfEmpty)));
    } else {
      found = support.collection(() -> to == null
          ? client.asyncBopGet(key, hex(from.hex(), 31), clientFilter, delete,
          isEnabled(dropIfEmpty))
          : client.asyncBopGet(key, hex(from.hex(), 31),
              hex(to.hex(), 31), clientFilter,
              defaultZero(offset), defaultZero(count), delete, isEnabled(dropIfEmpty)));
    }
    return new CollectionOperationResult<>(found.status(),
        found.value().values().stream().map(this::toElement).toList());
  }

  public CollectionOperationResult<Boolean> delete(
      String key, BTreeKey from, BTreeKey to,
      FlagFilter filter, Integer count, Boolean dropIfEmpty
  ) {
    var clientFilter = BTreeParameterMapper.toFilter(filter);
    return support.collection(() -> from.number() != null
        ? (to == null ? client.asyncBopDelete(key, from.number(),
        clientFilter,
        isEnabled(dropIfEmpty))
        : client.asyncBopDelete(key, from.number(), to.number(),
            clientFilter,
            defaultZero(count), isEnabled(dropIfEmpty)))
        : (to == null ? client.asyncBopDelete(key, hex(from.hex(), 31), clientFilter,
            isEnabled(dropIfEmpty))
            : client.asyncBopDelete(key, hex(from.hex(), 31),
                hex(to.hex(), 31), clientFilter,
                defaultZero(count), isEnabled(dropIfEmpty))));
  }

  public CollectionOperationResult<Integer> getItemCount(
      String key, BTreeKey from, BTreeKey to, FlagFilter filter
  ) {
    var clientFilter = BTreeParameterMapper.toFilter(filter);
    return support.collection(() -> from.number() != null
        ? client.asyncBopGetItemCount(key, from.number(),
        to.number(), clientFilter)
        : client.asyncBopGetItemCount(key, hex(from.hex(), 31),
            hex(to.hex(), 31),
            clientFilter));
  }

  private BTreeKey toBKey(Object key) {
    return key instanceof Long number ? new BTreeKey(number, null)
        : new BTreeKey(null, hex(((ByteArrayBKey) key).getBytes()));
  }

  public Map<String, CollectionOperationResult<List<BTreeElement>>> getBulk(
      List<String> keys, BTreeKey from, BTreeKey to,
      FlagFilter filter, Integer offset, Integer count
  ) {
    var clientFilter = BTreeParameterMapper.toFilter(filter);
    Map<String, ? extends BTreeGetResult<?, Object>> found;
    if (from.number() != null) {
      found = support.call(
          () -> client.asyncBopGetBulk(keys, from.number(),
              to.number(), clientFilter,
              defaultZero(offset), count));
    } else {
      found = support.call(
          () -> client.asyncBopGetBulk(keys, hex(from.hex(), 31),
              hex(to.hex(), 31),
              clientFilter, defaultZero(offset), count));
    }
    Map<String, CollectionOperationResult<List<BTreeElement>>> result = new LinkedHashMap<>();
    found.forEach((key, value) -> {
      var elements = value.getElements();
      List<BTreeElement> mapped = elements == null ? List.of() : elements.values().stream()
          .map(element -> new BTreeElement(toBKey(element.getBkey()), hex(element.getEflag()),
              element.getValue())).toList();
      result.put(key,
          new CollectionOperationResult<>(value.getCollectionResponse().getResponse().name(),
              mapped));
    });
    return result;
  }

  public CollectionOperationResult<SortMergeResult> smget(
      List<String> keys, BTreeKey from, BTreeKey to,
      FlagFilter filter, Integer count, Boolean unique
  ) {
    var clientFilter = BTreeParameterMapper.toFilter(filter);
    return support.result(() -> {
      var future = from.number() != null
          ? client.asyncBopSortMergeGet(keys, from.number(),
          to.number(), clientFilter,
          count, isEnabled(unique))
          : client.asyncBopSortMergeGet(keys, hex(from.hex(), 31),
              hex(to.hex(), 31),
              clientFilter, count, isEnabled(unique));
      var values = future.get();
      var status = future.getOperationStatus();
      if (status == null || !status.isSuccess()) {
        support.reject(status);
      }

      var elements = values.stream()
          .map(element -> new BTreeEntry(
              element.getKey(),
              new BTreeElement(from.number() != null
                  ? new BTreeKey(element.getBkey(), null)
                  : new BTreeKey(null, hex(element.getByteBkey())),
                  hex(element.getEflag()), element.getValue())))
          .toList();

      Map<String, String> missed = new LinkedHashMap<>();
      future.getMissedKeys().forEach((key, value) -> missed.put(key, value.getResponse().name()));
      var trimmed = future.getTrimmedKeys().stream()
          .map(element -> new TrimmedKey(
              element.getKey(),
              from.number() != null
                  ? new BTreeKey(element.getBkey(), null)
                  : new BTreeKey(null, hex(element.getByteBkey())))
          ).toList();

      return new CollectionOperationResult<>(status.getResponse().name(),
          new SortMergeResult(elements, missed, trimmed));
    });
  }

  public CollectionOperationResult<Integer> findPosition(
      String key, BTreeKey bKey, SortOrder order
  ) {
    return support.collection(() -> bKey.number() != null
        ? client.asyncBopFindPosition(key, bKey.number(), toClientOrder(order))
        : client.asyncBopFindPosition(key, hex(bKey.hex(), 31), toClientOrder(order)));
  }

  private CollectionOperationResult<Map<Integer, BTreeElement>> toPositions(
      CollectionOperationResult<Map<Integer, Element<Object>>> found) {
    Map<Integer, BTreeElement> mapped = new LinkedHashMap<>();
    found.value().forEach((position, value) -> mapped.put(position, toElement(value)));

    return new CollectionOperationResult<>(found.status(), mapped);
  }

  public CollectionOperationResult<Map<Integer, BTreeElement>> findPositionWithGet(
      String key, BTreeKey bKey, SortOrder order, Integer count
  ) {
    return toPositions(support.collection(() -> bKey.number() != null
        ? client.asyncBopFindPositionWithGet(key, bKey.number(),
        toClientOrder(order),
        count)
        : client.asyncBopFindPositionWithGet(key, hex(bKey.hex(), 31),
            toClientOrder(order),
            count)));
  }

  public CollectionOperationResult<Map<Integer, BTreeElement>> getByPosition(
      String key, SortOrder order, Integer from, Integer to
  ) {
    return toPositions(support.collection(() -> to == null
        ? client.asyncBopGetByPosition(key, toClientOrder(order), from)
        : client.asyncBopGetByPosition(key, toClientOrder(order), from,
            to)));
  }

  public PipedOperationResult pipedInsertBulk(
      String key, List<BTreeElement> elements, CollectionSettings attributesForCreate
  ) {
    var clientElements = elements.stream().map(
        element -> element.bKey().number() != null ? new Element<Object>(element.bKey().number(),
            element.value(),
            hex(element.eFlag(), 31))
            : new Element<Object>(hex(element.bKey().hex(), 31), element.value(),
                hex(element.eFlag(), 31))).toList();
    var attributes = toCreateAttributes(attributesForCreate);
    return support.piped(() -> client.asyncBopPipedInsertBulk(key, clientElements, attributes));
  }

  public PipedOperationResult pipedUpdateBulk(
      String key, List<BTreeElementUpdate> elements
  ) {
    var clientElements = elements.stream().map(
        element -> element.bKey().number() != null ? new Element<Object>(element.bKey().number(),
            element.value(),
            BTreeParameterMapper.toUpdate(element.eFlagUpdate()))
            : new Element<Object>(hex(element.bKey().hex(), 31), element.value(),
                BTreeParameterMapper.toUpdate(element.eFlagUpdate()))).toList();

    return support.piped(() -> client.asyncBopPipedUpdateBulk(key, clientElements));
  }

}
