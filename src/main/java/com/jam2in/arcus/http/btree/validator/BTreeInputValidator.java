package com.jam2in.arcus.http.btree.validator;

import com.jam2in.arcus.http.btree.model.BTreeKey;
import com.jam2in.arcus.http.btree.model.FlagComparison;
import com.jam2in.arcus.http.btree.model.FlagFilter;
import com.jam2in.arcus.http.btree.model.FlagUpdate;
import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import org.springframework.stereotype.Component;

@Component
public class BTreeInputValidator {

  public void validateKey(BTreeKey key) {
    requireValid(key != null && (key.number() == null) != (key.hex() == null));
    if (key.number() != null) {
      requireValid(key.number() >= 0);
    } else {
      validateHex(key.hex());
    }
  }

  public void validateRange(BTreeKey from, BTreeKey to) {
    validateKey(from);
    if (to != null) {
      validateKey(to);
      requireValid((from.number() == null) == (to.number() == null));
    }
  }

  public void validateHex(String value) {
    if (value != null) {
      requireValid(value.matches("(?:[0-9a-fA-F]{2}){1,31}"));
    }
  }

  public void validateFilter(FlagFilter filter) {
    if (filter == null) {
      return;
    }
    filter.values().forEach(this::validateHex);
    int length = filter.values().getFirst().length();
    requireValid(filter.values().stream().allMatch(value -> value.length() == length));
    requireValid((filter.offset() == null ? 0 : filter.offset()) + length / 2 <= 31);
    requireValid(filter.values().size() == 1 || filter.compare() == FlagComparison.Equal
        || filter.compare() == FlagComparison.NotEqual);
    requireValid((filter.bitOp() == null) == (filter.bitValue() == null));
    if (filter.bitValue() != null) {
      validateHex(filter.bitValue());
      requireValid(filter.bitValue().length() == length);
    }
  }

  public void validateUpdate(Object value, FlagUpdate update) {
    requireValid(value != null || update != null);
    if (update == null) {
      return;
    }
    if (Boolean.TRUE.equals(update.reset())) {
      requireValid(update.value() == null && update.offset() == null && update.bitOp() == null);
      return;
    }
    requireValid(update.value() != null);
    validateHex(update.value());
    if (update.bitOp() == null) {
      requireValid(update.offset() == null);
    } else {
      requireValid(
          (update.offset() == null ? 0 : update.offset()) + update.value().length() / 2 <= 31);
    }
  }

  private void requireValid(boolean valid) {
    if (!valid) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }
  }
}
