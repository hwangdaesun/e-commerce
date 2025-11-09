package com.side.hhplusecommerce.common.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 낙관적 락을 적용하는 어노테이션
 * AtomicLong 또는 AtomicInteger를 사용하여 버전 관리를 통한 동시성 제어
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OptimisticLock {
    /**
     * 재시도 최대 횟수
     */
    int maxRetries() default 3;

    /**
     * 재시도 간격
     */
    long retryDelay() default 50;

    /**
     * 재시도 간격 시간 단위
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;
}