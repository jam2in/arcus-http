package com.jam2in.arcus.http.btree.adapter;

import static com.jam2in.arcus.http.common.adapter.ClientSupport.hex;

import com.jam2in.arcus.http.btree.model.FlagFilter;
import com.jam2in.arcus.http.btree.model.FlagUpdate;
import com.jam2in.arcus.http.btree.model.SortOrder;
import net.spy.memcached.collection.BTreeOrder;
import net.spy.memcached.collection.ElementFlagFilter;
import net.spy.memcached.collection.ElementFlagUpdate;
import net.spy.memcached.collection.ElementMultiFlagsFilter;

final class BTreeParameterMapper {

  private BTreeParameterMapper() {
  }

  static int defaultZero(Integer value) {
    return value == null ? 0 : value;
  }

  static boolean isEnabled(Boolean value) {
    return Boolean.TRUE.equals(value);
  }

  static BTreeOrder toClientOrder(SortOrder order) {
    return order == SortOrder.ASC ? BTreeOrder.ASC : BTreeOrder.DESC;
  }

  static ElementFlagFilter toFilter(FlagFilter request) {
    if (request == null) {
      return null;
    }
    var values = request.values().stream().map(value -> hex(value, 31)).toList();
    ElementFlagFilter filter;
    var compare = ElementFlagFilter.CompOperands.valueOf(request.compare().name());
    if (values.size() == 1) {
      filter = new ElementFlagFilter(compare, values.getFirst());
    } else {
      var multi = new ElementMultiFlagsFilter().setCompOperand(compare);
      values.forEach(multi::addCompValue);
      filter = multi;
    }
    if (request.bitOp() != null) {
      filter.setBitOperand(ElementFlagFilter.BitWiseOperands.valueOf(request.bitOp().name()),
          hex(request.bitValue(), 31));
    }
    return filter.setCompareOffset(defaultZero(request.offset()));
  }

  static ElementFlagUpdate toUpdate(FlagUpdate request) {
    if (request == null) {
      return null;
    }
    if (isEnabled(request.reset())) {
      return ElementFlagUpdate.RESET_FLAG;
    }
    byte[] value = hex(request.value(), 31);
    return request.bitOp() == null ? new ElementFlagUpdate(value)
        : new ElementFlagUpdate(defaultZero(request.offset()),
            ElementFlagFilter.BitWiseOperands.valueOf(request.bitOp().name()), value);
  }
}
