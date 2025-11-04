package com.side.hhplusecommerce.user.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InsufficientPointException extends CustomException {
    public InsufficientPointException() {
        super(ErrorCode.INSUFFICIENT_POINT);
    }
}