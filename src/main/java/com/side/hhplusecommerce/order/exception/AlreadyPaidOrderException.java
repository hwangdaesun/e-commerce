package com.side.hhplusecommerce.order.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class AlreadyPaidOrderException extends CustomException {
    public AlreadyPaidOrderException() {
        super(ErrorCode.ALREADY_PAID_ORDER);
    }
}