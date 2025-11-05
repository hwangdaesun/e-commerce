package com.side.hhplusecommerce.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
@Schema(description = "사용자 쿠폰 목록 조회 응답")
public class UserCouponsResponse {
    @Schema(description = "사용자가 보유한 쿠폰 목록")
    private List<UserCoupon> coupons;

    @Getter
    @AllArgsConstructor
    @Schema(description = "사용자 쿠폰 정보")
    public static class UserCoupon {
        @Schema(description = "사용자 쿠폰 ID", example = "1")
        private Long userCouponId;

        @Schema(description = "쿠폰 ID", example = "1")
        private Long couponId;

        @Schema(description = "쿠폰명", example = "신규 가입 쿠폰")
        private String couponName;

        @Schema(description = "할인 금액", example = "5000")
        private Integer discountAmount;

        @Schema(description = "사용 여부", example = "false")
        private Boolean isUsed;

        @Schema(description = "사용일시 (사용하지 않은 경우 null)", example = "2025-10-25T14:30:00", nullable = true)
        private LocalDateTime usedAt;

        @Schema(description = "만료일시", example = "2025-12-30T23:59:59")
        private LocalDateTime expiresAt;

        @Schema(description = "발급일시", example = "2025-10-30T12:00:00")
        private LocalDateTime issuedAt;
    }

    public static UserCouponsResponse of(List<com.side.hhplusecommerce.coupon.domain.UserCoupon> userCoupons, Map<Long, com.side.hhplusecommerce.coupon.domain.Coupon> couponMap) {
        List<UserCoupon> coupons = userCoupons.stream()
                .map(userCoupon -> {
                    com.side.hhplusecommerce.coupon.domain.Coupon coupon = couponMap.get(userCoupon.getCouponId());
                    return new UserCoupon(
                            userCoupon.getUserCouponId(),
                            coupon.getCouponId(),
                            coupon.getName(),
                            coupon.getDiscountAmount(),
                            userCoupon.getIsUsed(),
                            userCoupon.getUsedAt(),
                            coupon.getExpiresAt(),
                            userCoupon.getIssuedAt()
                    );
                })
                .toList();
        return new UserCouponsResponse(coupons);
    }
}