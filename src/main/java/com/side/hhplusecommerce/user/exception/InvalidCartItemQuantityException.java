package com.side.hhplusecommerce.user.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InvalidCartItemQuantityException extends CustomException {
    public InvalidCartItemQuantityException() {
        super(ErrorCode.INVALID_CART_ITEM_QUANTITY);
    }
}