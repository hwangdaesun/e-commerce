package com.side.hhplusecommerce.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 비관적 락(배타 락)을 적용하는 어노테이션
 * ReentrantLock을 사용하여 동시성 제어
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PessimisticLock {
    /**
     * 락 획득 대기 시간
     */
    long timeout() default 1;

    /**
     * 락 획득 대기 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}