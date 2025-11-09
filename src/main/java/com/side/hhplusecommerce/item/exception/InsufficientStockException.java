package com.side.hhplusecommerce.item.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InsufficientStockException extends CustomException {
    public InsufficientStockException() {
        super(ErrorCode.INSUFFICIENT_STOCK);
    }
}
