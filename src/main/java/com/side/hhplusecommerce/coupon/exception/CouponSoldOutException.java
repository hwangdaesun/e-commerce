package com.side.hhplusecommerce.coupon.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class CouponSoldOutException extends CustomException {
    public CouponSoldOutException() {
        super(ErrorCode.COUPON_SOLD_OUT);
    }
}