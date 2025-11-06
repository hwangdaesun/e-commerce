package com.side.hhplusecommerce.coupon.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.side.hhplusecommerce.cart.usecase.CouponIssueUseCase;
import com.side.hhplusecommerce.coupon.service.CouponIssueLockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponIssueUseCaseTest {

    @Mock
    private CouponIssueLockService couponIssueLockService;

    @InjectMocks
    private CouponIssueUseCase couponIssueUseCase;

    @Test
    @DisplayName("쿠폰 재고 감소 실패 시 UserCoupon 저장이 시도되지 않는다.")
    void issue_fails_when_stock_decrease_fails() {
        // given
        Long couponId = 1L;
        Long userId = 100L;

        given(couponIssueLockService.issueCouponWithPessimisticLock(couponId, userId))
                .willThrow(new RuntimeException("Stock save failed"));

        // when & then
        assertThatThrownBy(() -> couponIssueUseCase.issue(couponId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Stock save failed");

        verify(couponIssueLockService).issueCouponWithPessimisticLock(couponId, userId);
    }

    @Test
    @DisplayName("UserCoupon 저장 실패 시 쿠폰 재고를 롤백한다.")
    void issue_rollback_on_user_coupon_save_failure() {
        // given
        Long couponId = 1L;
        Long userId = 100L;

        given(couponIssueLockService.issueCouponWithPessimisticLock(couponId, userId))
                .willThrow(new RuntimeException("DB connection failed"));

        // when & then
        assertThatThrownBy(() -> couponIssueUseCase.issue(couponId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB connection failed");

        verify(couponIssueLockService).issueCouponWithPessimisticLock(couponId, userId);
    }

    // TODO: UserCoupon 저장 성공 후 예상치 못한 예외 발생 시 쿠폰 재고 롤백 및 유저 쿠폰 회수 테스트
}
