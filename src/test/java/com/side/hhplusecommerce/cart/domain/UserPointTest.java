package com.side.hhplusecommerce.cart.domain;

import com.side.hhplusecommerce.cart.domain.UserPoint;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.cart.exception.InsufficientPointException;
import com.side.hhplusecommerce.cart.exception.InvalidPointAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class UserPointTest {

    @Test
    @DisplayName("포인트를 충전한다")
    void charge_success() {
        // given
        UserPoint userPoint = UserPoint.initialize(1L);
        Integer chargeAmount = 10000;

        // when
        userPoint.charge(chargeAmount);

        // then
        assertThat(userPoint.getPoint()).isEqualTo(10000);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -1000})
    @DisplayName("충전 금액이 1 미만이면 실패한다")
    void charge_fail_invalid_amount(int amount) {
        // given
        UserPoint userPoint = UserPoint.initialize(1L);

        // when & then
        assertThatThrownBy(() -> userPoint.charge(amount))
                .isInstanceOf(InvalidPointAmountException.class)
                .hasMessage(ErrorCode.INVALID_POINT_AMOUNT.getMessage());
    }


    @ParameterizedTest
    @ValueSource(ints = {0, -1, -1000})
    @DisplayName("사용 금액이 1 미만이면 실패한다")
    void use_fail_invalid_amount(int amount) {
        // given
        UserPoint userPoint = UserPoint.initialize(1L);
        userPoint.charge(10000);

        // when & then
        assertThatThrownBy(() -> userPoint.use(amount))
                .isInstanceOf(InvalidPointAmountException.class)
                .hasMessage(ErrorCode.INVALID_POINT_AMOUNT.getMessage());
    }

    @Test
    @DisplayName("포인트가 부족하면 사용에 실패한다")
    void use_fail_insufficient_point() {
        // given
        UserPoint userPoint = UserPoint.initialize(1L);
        userPoint.charge(5000);

        // when & then
        assertThatThrownBy(() -> userPoint.use(10000))
                .isInstanceOf(InsufficientPointException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());
    }

    @Test
    @DisplayName("포인트가 0일 때 사용하면 실패한다")
    void use_fail_zero_point() {
        // given
        UserPoint userPoint = UserPoint.initialize(1L);

        // when & then
        assertThatThrownBy(() -> userPoint.use(1000))
                .isInstanceOf(InsufficientPointException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());
    }

}
