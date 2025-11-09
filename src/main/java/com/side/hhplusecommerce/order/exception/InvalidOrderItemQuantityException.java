package com.side.hhplusecommerce.order.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InvalidOrderItemQuantityException extends CustomException {
    public InvalidOrderItemQuantityException() {
        super(ErrorCode.INVALID_ORDER_ITEM_QUANTITY);
    }
}
