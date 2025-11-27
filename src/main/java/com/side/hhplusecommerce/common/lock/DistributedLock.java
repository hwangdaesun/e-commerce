package com.side.hhplusecommerce.common.lock.distributed;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 분산 락을 적용하기 위한 어노테이션
 * Redisson을 사용하여 분산 환경에서 동시성을 제어합니다.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 락 키를 생성하는 LockKeyResolver 빈 이름
     * 예: "userPointLockKeyResolver"
     */
    String keyResolver();

    /**
     * 락 키에 사용할 값을 추출하는 SpEL 표현식
     * 예: "#userId", "#request.userId"
     */
    String key();

    /**
     * 락 획득을 시도하는 최대 대기 시간
     * 기본값: 5초
     */
    long waitTime() default 5L;

    /**
     * 락을 획득한 후 자동으로 해제되는 시간 (데드락 방지)
     * 기본값: 3초
     */
    long leaseTime() default 3L;

    /**
     * 시간 단위
     * 기본값: 초(SECONDS)
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}
