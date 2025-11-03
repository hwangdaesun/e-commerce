package com.side.hhplusecommerce.order.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InvalidOrderItemPriceException extends CustomException {
    public InvalidOrderItemPriceException() {
        super(ErrorCode.INVALID_ORDER_ITEM_PRICE);
    }
}
