package com.side.hhplusecommerce.user.domain;

import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.user.exception.InvalidPointAmountException;
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

}
