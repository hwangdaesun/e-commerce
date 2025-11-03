package com.side.hhplusecommerce.order.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InvalidOrderItemOrderIdException extends CustomException {
    public InvalidOrderItemOrderIdException() {
        super(ErrorCode.INVALID_ORDER_ITEM_ORDER_ID);
    }
}