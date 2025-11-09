package com.side.hhplusecommerce.coupon.domain;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.exception.CouponSoldOutException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CouponStockTest {

    @Test
    @DisplayName("쿠폰 재고를 정상적으로 차감한다")
    void decrease_success() {
        // given
        CouponStock couponStock = CouponStock.of(1L, 10);

        // when
        couponStock.decrease();

        // then
        assertThat(couponStock.getRemainingQuantity()).isEqualTo(9);
    }

    @Test
    @DisplayName("쿠폰 재고가 0일 때 차감하면 예외를 발생시킨다")
    void decrease_fail_sold_out() {
        // given
        CouponStock couponStock = CouponStock.of(1L, 0);

        // when & then
        assertThatThrownBy(couponStock::decrease)
                .isInstanceOf(CouponSoldOutException.class)
                .hasMessage(ErrorCode.COUPON_SOLD_OUT.getMessage());

        // 재고가 변경되지 않았는지 확인
        assertThat(couponStock.getRemainingQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("남은 수량이 있는지 확인할 수 있다")
    void hasRemainingQuantity() {
        // given
        CouponStock couponStockWithStock = CouponStock.of(1L, 5);
        CouponStock couponStockSoldOut = CouponStock.of(2L, 0);

        // when & then
        assertThat(couponStockWithStock.hasRemainingQuantity()).isTrue();
        assertThat(couponStockSoldOut.hasRemainingQuantity()).isFalse();
    }
}