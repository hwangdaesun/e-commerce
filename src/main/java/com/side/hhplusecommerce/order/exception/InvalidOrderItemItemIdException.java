package com.side.hhplusecommerce.order.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InvalidOrderItemItemIdException extends CustomException {
    public InvalidOrderItemItemIdException() {
        super(ErrorCode.INVALID_ORDER_ITEM_ITEM_ID);
    }
}