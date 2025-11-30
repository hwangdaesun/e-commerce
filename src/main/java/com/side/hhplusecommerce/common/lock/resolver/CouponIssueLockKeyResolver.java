package com.side.hhplusecommerce.common.lock.resolver;

import com.side.hhplusecommerce.common.lock.LockKeyResolver;
import com.side.hhplusecommerce.common.lock.LockKeyType;
import org.springframework.stereotype.Component;

/**
 * 쿠폰 발급 락 키 생성 Resolver
 * 쿠폰 발급 시 couponId 기반으로 락 키를 생성합니다.
 */
@Component("couponIssueLockKeyResolver")
public class CouponIssueLockKeyResolver implements LockKeyResolver {

    @Override
    public String getKeyPrefix() {
        return LockKeyType.COUPON.getPrefix();
    }
}