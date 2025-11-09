package com.side.hhplusecommerce.common.lock.exception;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;

/**
 * 락 처리 중 인터럽트 발생 예외
 */
public class LockInterruptedException extends CustomException {

    public LockInterruptedException(Throwable cause) {
        super(ErrorCode.LOCK_INTERRUPTED, cause);
    }
}