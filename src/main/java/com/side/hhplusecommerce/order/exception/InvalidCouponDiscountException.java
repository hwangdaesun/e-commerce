package com.side.hhplusecommerce.order.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class InvalidCouponDiscountException extends CustomException {
    public InvalidCouponDiscountException() {
        super(ErrorCode.INVALID_COUPON_DISCOUNT);
    }
}