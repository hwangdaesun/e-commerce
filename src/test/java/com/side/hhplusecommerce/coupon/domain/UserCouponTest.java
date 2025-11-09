package com.side.hhplusecommerce.coupon.domain;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.exception.AlreadyUsedCouponException;
import com.side.hhplusecommerce.coupon.exception.ExpiredCouponException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class UserCouponTest {

    @Test
    @DisplayName("이미 사용된 쿠폰은 다시 사용할 수 없다")
    void use_fail_already_used() {
        // given
        UserCoupon userCoupon = UserCoupon.issue(1L, 1L);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        userCoupon.use(expiresAt);

        // when & then
        assertThatThrownBy(() -> userCoupon.use(expiresAt))
                .isInstanceOf(AlreadyUsedCouponException.class)
                .hasMessage(ErrorCode.ALREADY_USED_COUPON.getMessage());
    }

    @Test
    @DisplayName("만료된 쿠폰은 사용할 수 없다")
    void use_fail_expired() {
        // given
        UserCoupon userCoupon = UserCoupon.issue(1L, 1L);
        LocalDateTime expiresAt = LocalDateTime.now().minusDays(1);

        // when & then
        assertThatThrownBy(() -> userCoupon.use(expiresAt))
                .isInstanceOf(ExpiredCouponException.class)
                .hasMessage(ErrorCode.EXPIRED_COUPON.getMessage());
    }
}
