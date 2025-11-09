package com.side.hhplusecommerce.item.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InvalidSalesQuantityException extends CustomException {
    public InvalidSalesQuantityException() {
        super(ErrorCode.INVALID_SALES_QUANTITY);
    }
}