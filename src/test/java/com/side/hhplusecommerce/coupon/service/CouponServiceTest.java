package com.side.hhplusecommerce.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.exception.AlreadyUsedCouponException;
import com.side.hhplusecommerce.coupon.exception.ExpiredCouponException;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import com.side.hhplusecommerce.coupon.service.dto.CouponUseResult;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("유효한 쿠폰을 사용한다")
    void useCoupon_success() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;
        Long couponId = 10L;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        UserCoupon userCoupon = UserCoupon.createWithId(
                userCouponId, userId, couponId, false, null, LocalDateTime.now()
        );

        Coupon coupon = Coupon.builder()
                .couponId(couponId)
                .name("할인 쿠폰")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(expiresAt)
                .build();

        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        // when
        CouponUseResult result = couponService.useCoupon(userCouponId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getCoupon()).isEqualTo(coupon);
        assertThat(result.getDiscountAmount()).isEqualTo(5000);
        assertThat(userCoupon.getIsUsed()).isTrue();
        assertThat(userCoupon.getUsedAt()).isNotNull();

        verify(userCouponRepository).findById(userCouponId);
        verify(couponRepository).findById(couponId);
        verify(userCouponRepository).save(userCoupon);
    }

    @Test
    @DisplayName("사용자 쿠폰이 존재하지 않으면 예외를 발생시킨다")
    void useCoupon_fail_user_coupon_not_found() {
        // given
        Long userCouponId = 1L;
        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(userCouponId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COUPON_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("쿠폰이 존재하지 않으면 예외를 발생시킨다")
    void useCoupon_fail_coupon_not_found() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;
        Long couponId = 10L;

        UserCoupon userCoupon = UserCoupon.createWithId(
                userCouponId, userId, couponId, false, null, LocalDateTime.now()
        );

        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(couponId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(userCouponId))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorCode.COUPON_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("이미 사용된 쿠폰은 사용할 수 없다")
    void useCoupon_fail_already_used() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;
        Long couponId = 10L;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        UserCoupon userCoupon = UserCoupon.createWithId(
                userCouponId, userId, couponId, true, LocalDateTime.now().minusDays(1), LocalDateTime.now().minusDays(10)
        );

        Coupon coupon = Coupon.builder()
                .couponId(couponId)
                .name("할인 쿠폰")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(expiresAt)
                .build();

        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(userCouponId))
                .isInstanceOf(AlreadyUsedCouponException.class);
    }

    @Test
    @DisplayName("만료된 쿠폰은 사용할 수 없다")
    void useCoupon_fail_expired() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;
        Long couponId = 10L;
        LocalDateTime expiresAt = LocalDateTime.now().minusDays(1); // 만료됨

        UserCoupon userCoupon = UserCoupon.createWithId(
                userCouponId, userId, couponId, false, null, LocalDateTime.now().minusDays(10)
        );

        Coupon coupon = Coupon.builder()
                .couponId(couponId)
                .name("할인 쿠폰")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(expiresAt)
                .build();

        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        // when & then
        assertThatThrownBy(() -> couponService.useCoupon(userCouponId))
                .isInstanceOf(ExpiredCouponException.class);
    }

    @Test
    @DisplayName("할인 금액이 큰 쿠폰도 정상적으로 사용된다")
    void useCoupon_success_large_discount() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;
        Long couponId = 10L;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);

        UserCoupon userCoupon = UserCoupon.createWithId(
                userCouponId, userId, couponId, false, null, LocalDateTime.now()
        );

        Coupon coupon = Coupon.builder()
                .couponId(couponId)
                .name("대박 할인 쿠폰")
                .discountAmount(50000)
                .totalQuantity(10)
                .expiresAt(expiresAt)
                .build();

        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        // when
        CouponUseResult result = couponService.useCoupon(userCouponId);

        // then
        assertThat(result.getDiscountAmount()).isEqualTo(50000);
        assertThat(result.getCoupon().getName()).isEqualTo("대박 할인 쿠폰");
    }

    @Test
    @DisplayName("만료일이 오늘인 쿠폰은 사용할 수 있다")
    void useCoupon_success_expires_today() {
        // given
        Long userCouponId = 1L;
        Long userId = 1L;
        Long couponId = 10L;
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1); // 1시간 후 만료

        UserCoupon userCoupon = UserCoupon.createWithId(
                userCouponId, userId, couponId, false, null, LocalDateTime.now().minusDays(1)
        );

        Coupon coupon = Coupon.builder()
                .couponId(couponId)
                .name("오늘까지 쿠폰")
                .discountAmount(3000)
                .totalQuantity(50)
                .expiresAt(expiresAt)
                .build();

        given(userCouponRepository.findById(userCouponId)).willReturn(Optional.of(userCoupon));
        given(couponRepository.findById(couponId)).willReturn(Optional.of(coupon));

        // when
        CouponUseResult result = couponService.useCoupon(userCouponId);

        // then
        assertThat(result).isNotNull();
        assertThat(userCoupon.getIsUsed()).isTrue();
    }
}
