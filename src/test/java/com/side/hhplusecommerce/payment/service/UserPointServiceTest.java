package com.side.hhplusecommerce.payment.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.point.domain.UserPoint;
import com.side.hhplusecommerce.point.exception.InsufficientPointException;
import com.side.hhplusecommerce.point.exception.InvalidPointAmountException;
import com.side.hhplusecommerce.point.repository.UserPointRepository;
import java.util.Optional;
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
    private UserPointRepository userPointRepository;

    @InjectMocks
    private UserPointService userPointService;

    @Test
    @DisplayName("사용자 포인트를 차감한다")
    void use_Point_success() {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        UserPoint userPoint = UserPoint.initialize(userId);
        userPoint.charge(50000); // 충분한 잔액

        when(userPointRepository.findByUserId(userId)).thenReturn(Optional.of(userPoint));

        // when
        userPointService.use(userId, amount);

        // then
        verify(userPointRepository).findByUserId(userId);
        verify(userPointRepository).save(any(UserPoint.class));
    }

    @Test
    @DisplayName("사용자 포인트가 없으면 예외를 발생시킨다")
    void use_Point_fail_user_point_not_found() {
        // given
        Long userId = 1L;
        Integer amount = 10000;

        when(userPointRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userPointService.use(userId, amount))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_POINT_NOT_FOUND.getMessage());

        verify(userPointRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("포인트가 부족하면 예외를 발생시킨다")
    void use_Point_fail_insufficient_point() {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        UserPoint userPoint = UserPoint.initialize(userId);
        userPoint.charge(5000); // 부족한 잔액

        when(userPointRepository.findByUserId(userId)).thenReturn(Optional.of(userPoint));

        // when & then
        assertThatThrownBy(() -> userPointService.use(userId, amount))
                .isInstanceOf(InsufficientPointException.class);

        verify(userPointRepository).findByUserId(userId);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, -1000})
    @DisplayName("차감 금액이 0 이하면 예외를 발생시킨다")
    void use_Point_fail_invalid_amount(Integer amount) {
        // given
        Long userId = 1L;
        UserPoint userPoint = UserPoint.initialize(userId);
        userPoint.charge(50000);

        when(userPointRepository.findByUserId(userId)).thenReturn(Optional.of(userPoint));

        // when & then
        assertThatThrownBy(() -> userPointService.use(userId, amount))
                .isInstanceOf(InvalidPointAmountException.class);
    }

    @Test
    @DisplayName("사용자 포인트를 충전한다")
    void charge_Point_success() {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        UserPoint userPoint = UserPoint.initialize(userId);

        when(userPointRepository.findByUserId(userId)).thenReturn(Optional.of(userPoint));

        // when
        userPointService.charge(userId, amount);

        // then
        verify(userPointRepository).findByUserId(userId);
        verify(userPointRepository).save(any(UserPoint.class));
    }

    @Test
    @DisplayName("충전 시 사용자 포인트가 없으면 예외를 발생시킨다")
    void charge_Point_fail_user_point_not_found() {
        // given
        Long userId = 1L;
        Integer amount = 10000;

        when(userPointRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userPointService.charge(userId, amount))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_POINT_NOT_FOUND.getMessage());

        verify(userPointRepository).findByUserId(userId);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, -1000})
    @DisplayName("충전 금액이 0 이하면 예외를 발생시킨다")
    void charge_Point_fail_invalid_amount(Integer amount) {
        // given
        Long userId = 1L;
        UserPoint userPoint = UserPoint.initialize(userId);

        when(userPointRepository.findByUserId(userId)).thenReturn(Optional.of(userPoint));

        // when & then
        assertThatThrownBy(() -> userPointService.charge(userId, amount))
                .isInstanceOf(InvalidPointAmountException.class);
    }
}
