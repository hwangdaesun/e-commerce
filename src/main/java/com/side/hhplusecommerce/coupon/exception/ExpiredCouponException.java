package com.side.hhplusecommerce.coupon.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class ExpiredCouponException extends CustomException {
    public ExpiredCouponException() {
        super(ErrorCode.EXPIRED_COUPON);
    }
}