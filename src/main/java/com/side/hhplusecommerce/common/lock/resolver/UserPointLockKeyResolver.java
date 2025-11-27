package com.side.hhplusecommerce.common.lock.resolver;

import com.side.hhplusecommerce.common.lock.LockKeyResolver;
import com.side.hhplusecommerce.common.lock.LockKeyType;
import org.springframework.stereotype.Component;

/**
 * 사용자 포인트 락 키 생성 Resolver
 * 포인트 충전/사용 시 사용되는 락 키의 프리픽스를 제공합니다.
 */
@Component("userPointLockKeyResolver")
public class UserPointLockKeyResolver implements LockKeyResolver {

    @Override
    public String getKeyPrefix() {
        return LockKeyType.USER_POINT.getPrefix();
    }
}
