package com.side.hhplusecommerce.coupon.domain;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CouponIssueValidatorTest {

    private final CouponIssueValidator couponIssueValidator = new CouponIssueValidator();

    @Test
    @DisplayName("이미 발급받은 쿠폰인 경우 예외를 발생시킨다")
    void validateNotAlreadyIssued_fail_already_issued() {
        // given
        Long couponId = 1L;
        Long userId = 1L;

        UserCoupon userCoupon = UserCoupon.createWithId(
                1L, userId, couponId, false, null, LocalDateTime.now()
        );
        List<UserCoupon> userCoupons = List.of(userCoupon);

        // when & then
        assertThatThrownBy(() -> couponIssueValidator.validateNotAlreadyIssued(couponId, userCoupons))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ALREADY_ISSUED_COUPON.getMessage());
    }

    @Test
    @DisplayName("발급받지 않은 쿠폰인 경우 예외가 발생하지 않는다")
    void validateNotAlreadyIssued_success() {
        // given
        Long couponId = 1L;
        Long userId = 1L;

        UserCoupon userCoupon = UserCoupon.createWithId(
                1L, userId, 2L, false, null, LocalDateTime.now()
        );
        List<UserCoupon> userCoupons = List.of(userCoupon);

        // when & then
        assertThatCode(() -> couponIssueValidator.validateNotAlreadyIssued(couponId, userCoupons))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("여러 쿠폰 중 해당 쿠폰이 포함된 경우 예외를 발생시킨다")
    void validateNotAlreadyIssued_fail_multiple_coupons() {
        // given
        Long targetCouponId = 2L;
        Long userId = 1L;

        UserCoupon userCoupon1 = UserCoupon.createWithId(
                1L, userId, 1L, false, null, LocalDateTime.now()
        );
        UserCoupon userCoupon2 = UserCoupon.createWithId(
                2L, userId, targetCouponId, false, null, LocalDateTime.now()
        );
        UserCoupon userCoupon3 = UserCoupon.createWithId(
                3L, userId, 3L, false, null, LocalDateTime.now()
        );
        List<UserCoupon> userCoupons = List.of(userCoupon1, userCoupon2, userCoupon3);

        // when & then
        assertThatThrownBy(() -> couponIssueValidator.validateNotAlreadyIssued(targetCouponId, userCoupons))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.ALREADY_ISSUED_COUPON.getMessage());
    }
}
