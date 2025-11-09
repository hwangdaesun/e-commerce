package com.side.hhplusecommerce.common.lock.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

/**
 * 락 획득 타임아웃 예외
 */
public class LockTimeoutException extends CustomException {

    public LockTimeoutException() {
        super(ErrorCode.LOCK_TIMEOUT);
    }

    public LockTimeoutException(String message) {
        super(ErrorCode.LOCK_TIMEOUT, message);
    }
}