package com.side.hhplusecommerce.user.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InvalidPointAmountException extends CustomException {
    public InvalidPointAmountException() {
        super(ErrorCode.INVALID_POINT_AMOUNT);
    }
}