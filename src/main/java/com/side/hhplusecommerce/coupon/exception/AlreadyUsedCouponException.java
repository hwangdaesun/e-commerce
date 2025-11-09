package com.side.hhplusecommerce.coupon.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

public class AlreadyUsedCouponException extends CustomException {
    public AlreadyUsedCouponException() {
        super(ErrorCode.ALREADY_USED_COUPON);
    }
}