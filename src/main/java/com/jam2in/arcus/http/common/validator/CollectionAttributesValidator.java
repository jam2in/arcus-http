package com.jam2in.arcus.http.common.validator;

import com.jam2in.arcus.http.common.exception.ErrorCode;
import com.jam2in.arcus.http.common.exception.ArcusHttpException;
import com.jam2in.arcus.http.common.model.CollectionKind;
import com.jam2in.arcus.http.common.model.CollectionSettings;
import com.jam2in.arcus.http.common.model.OverflowPolicy;
import org.springframework.stereotype.Component;

@Component
public class CollectionAttributesValidator {

  public void validateForCreate(CollectionSettings attributes, CollectionKind collectionKind) {
    requireValid(collectionKind != null);
    if (attributes == null) {
      return;
    }

    validateBKeyRange(attributes.maxBKeyRange(), attributes.maxBKeyRangeHex());
    requireValid(collectionKind == CollectionKind.BTREE
        || (attributes.maxBKeyRange() == null && attributes.maxBKeyRangeHex() == null));

    if (attributes.overflowAction() != null) {
      boolean allowed = switch (collectionKind) {
        case LIST -> switch (attributes.overflowAction()) {
          case error, head_trim, tail_trim -> true;
          default -> false;
        };
        case SET, MAP -> attributes.overflowAction() == OverflowPolicy.error;
        case BTREE -> switch (attributes.overflowAction()) {
          case error, smallest_trim, largest_trim, smallest_silent_trim, largest_silent_trim ->
              true;
          default -> false;
        };
      };
      requireValid(allowed);
    }
  }

  public void validateForUpdate(CollectionSettings attributes) {
    requireValid(attributes != null);
    requireValid(attributes.expireTime() != null || attributes.maxCount() != null
        || attributes.readable() != null || attributes.overflowAction() != null
        || attributes.maxBKeyRange() != null || attributes.maxBKeyRangeHex() != null);
    validateBKeyRange(attributes.maxBKeyRange(), attributes.maxBKeyRangeHex());
    // The server checks type-specific constraints against the existing item.
  }

  private void validateBKeyRange(Long number, String hex) {
    requireValid(number == null || hex == null);
    if (hex != null) {
      requireValid(hex.matches("(?:[0-9a-fA-F]{2}){1,31}"));
    }
  }

  private void requireValid(boolean valid) {
    if (!valid) {
      throw new ArcusHttpException(ErrorCode.INVALID_REQUEST);
    }
  }
}
