package com.side.hhplusecommerce.common.lock;

/**
 * 분산 락 키의 프리픽스를 제공하는 Resolver 인터페이스
 * 각 도메인별로 구현체를 작성하여 빈으로 등록합니다.
 */
@FunctionalInterface
public interface LockKeyResolver {

    /**
     * 락 키의 프리픽스를 반환합니다.
     *
     * @return 락 키 프리픽스 (예: "user-point")
     */
    String getKeyPrefix();
}
