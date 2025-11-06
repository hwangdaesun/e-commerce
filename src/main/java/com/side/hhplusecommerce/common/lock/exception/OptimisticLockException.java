package com.side.hhplusecommerce.common.lock.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

/**
 * 낙관적 락 충돌 예외
 */
public class OptimisticLockException extends CustomException {

    public OptimisticLockException() {
        super(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
    }

    public OptimisticLockException(String message) {
        super(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, message);
    }
}