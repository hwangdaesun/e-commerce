package com.side.hhplusecommerce.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.point.exception.InsufficientPointException;
import com.side.hhplusecommerce.point.exception.InvalidPointAmountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserPointServiceTest {

    @Mock
    private UserPointLockService userPointLockService;

    @InjectMocks
    private UserPointService userPointService;

    @Test
    @DisplayName("사용자 포인트를 차감한다")
    void use_PointWithPessimisticLock_success() {
        // given
        Long userId = 1L;
        Integer amount = 10000;

        // when
        userPointService.use(userId, amount);

        // then
        verify(userPointLockService).usePointWithPessimisticLock(userId, amount);
    }

    @Test
    @DisplayName("사용자 포인트가 없으면 예외를 발생시킨다")
    void use_PointWithPessimisticLock_fail_user_point_not_found() {
        // given
        Long userId = 1L;
        Integer amount = 10000;

        doThrow(new CustomException(ErrorCode.USER_POINT_NOT_FOUND))
                .when(userPointLockService).usePointWithPessimisticLock(userId, amount);

        // when & then
        assertThatThrownBy(() -> userPointService.use(userId, amount))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_POINT_NOT_FOUND.getMessage());

        verify(userPointLockService).usePointWithPessimisticLock(userId, amount);
    }

    @Test
    @DisplayName("포인트가 부족하면 예외를 발생시킨다")
    void use_PointWithPessimisticLock_fail_insufficient_point() {
        // given
        Long userId = 1L;
        Integer amount = 10000;

        doThrow(new InsufficientPointException())
                .when(userPointLockService).usePointWithPessimisticLock(userId, amount);

        // when & then
        assertThatThrownBy(() -> userPointService.use(userId, amount))
                .isInstanceOf(InsufficientPointException.class);

        verify(userPointLockService).usePointWithPessimisticLock(userId, amount);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, -1000})
    @DisplayName("차감 금액이 0 이하면 예외를 발생시킨다")
    void use_PointWithPessimisticLock_fail_invalid_amount(Integer amount) {
        // given
        Long userId = 1L;

        doThrow(new InvalidPointAmountException())
                .when(userPointLockService).usePointWithPessimisticLock(userId, amount);

        // when & then
        assertThatThrownBy(() -> userPointService.use(userId, amount))
                .isInstanceOf(InvalidPointAmountException.class);
    }
}
