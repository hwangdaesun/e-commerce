package com.side.hhplusecommerce.coupon.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.side.hhplusecommerce.cart.usecase.CouponIssueUseCase;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.CouponIssueValidator;
import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponIssueUseCaseTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponStockRepository couponStockRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponIssueValidator couponIssueValidator;

    @InjectMocks
    private CouponIssueUseCase couponIssueUseCase;

    @Test
    @DisplayName("UserCoupon 저장 실패 시 쿠폰 재고를 롤백한다.")
    void issue_rollback_on_user_coupon_save_failure() {
        // given
        Long couponId = 1L;
        Long userId = 100L;
        Integer initialQuantity = 10;

        Coupon coupon = Coupon.builder()
                .couponId(couponId)
                .name("Test Coupon")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        CouponStock couponStock = CouponStock.of(couponId, initialQuantity);

        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));
        given(userCouponRepository.findByUserId(userId)).willReturn(List.of());
        given(couponStockRepository.findByCouponId(couponId)).willReturn(Optional.of(couponStock));

        // UserCoupon 저장 시 예외 발생
        doThrow(new RuntimeException("DB connection failed"))
                .when(userCouponRepository).save(any(UserCoupon.class));

        // 저장에 실패했으므로 롤백 시 발급된 쿠폰이 없어야 함
        given(userCouponRepository.findByUserIdAndCouponId(userId, couponId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponIssueUseCase.issue(couponId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB connection failed");

        // 재고 차감 1회 (decrease), 롤백 시 재고 복구 1회 (increase) → 총 2회 save
        verify(couponStockRepository, times(2)).save(couponStock);
        verify(userCouponRepository).save(any(UserCoupon.class));

        // 롤백 로직에서 발급된 쿠폰 조회 확인 (저장 실패했으므로 쿠폰이 없음)
        verify(userCouponRepository).findByUserIdAndCouponId(userId, couponId);
        // 쿠폰이 없으므로 delete는 호출되지 않음
        verify(userCouponRepository, never()).delete(any(UserCoupon.class));

        // 재고가 원상복구되었는지 확인
        assertThat(couponStock.getRemainingQuantity()).isEqualTo(initialQuantity);
    }

    // TODO: UserCoupon 저장 성공 후 예상치 못한 예외 발생 시 쿠폰 재고 롤백 및 유저 쿠폰 회수 테스트
}
