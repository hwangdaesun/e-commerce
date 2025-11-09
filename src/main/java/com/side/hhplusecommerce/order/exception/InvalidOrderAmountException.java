package com.side.hhplusecommerce.order.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InvalidOrderAmountException extends CustomException {
    public InvalidOrderAmountException() {
        super(ErrorCode.INVALID_ORDER_AMOUNT);
    }
}