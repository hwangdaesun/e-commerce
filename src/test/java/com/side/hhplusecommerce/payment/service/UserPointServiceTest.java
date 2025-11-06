package com.side.hhplusecommerce.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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
    void use_success() {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        Integer initialPoint = 50000;

        UserPoint userPoint = UserPoint.initialize(userId);
        userPoint.charge(initialPoint);

        given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));

        // when
        userPointService.use(userId, amount);

        // then
        assertThat(userPoint.getPoint()).isEqualTo(40000);
        verify(userPointRepository).findByUserId(userId);
        verify(userPointRepository).save(userPoint);
    }

    @Test
    @DisplayName("사용자 포인트가 없으면 예외를 발생시킨다")
    void use_fail_user_point_not_found() {
        // given
        Long userId = 1L;
        Integer amount = 10000;

        given(userPointRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userPointService.use(userId, amount))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.USER_POINT_NOT_FOUND.getMessage());

        verify(userPointRepository).findByUserId(userId);
    }

    @Test
    @DisplayName("포인트가 부족하면 예외를 발생시킨다")
    void use_fail_insufficient_point() {
        // given
        Long userId = 1L;
        Integer amount = 10000;
        Integer initialPoint = 5000;

        UserPoint userPoint = UserPoint.initialize(userId);
        userPoint.charge(initialPoint);

        given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));

        // when & then
        assertThatThrownBy(() -> userPointService.use(userId, amount))
                .isInstanceOf(InsufficientPointException.class);

        verify(userPointRepository).findByUserId(userId);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, -1000})
    @DisplayName("차감 금액이 0 이하면 예외를 발생시킨다")
    void use_fail_invalid_amount(Integer amount) {
        // given
        Long userId = 1L;
        Integer initialPoint = 10000;

        UserPoint userPoint = UserPoint.initialize(userId);
        userPoint.charge(initialPoint);

        given(userPointRepository.findByUserId(userId)).willReturn(Optional.of(userPoint));

        // when & then
        assertThatThrownBy(() -> userPointService.use(userId, amount))
                .isInstanceOf(InvalidPointAmountException.class);
    }
}
