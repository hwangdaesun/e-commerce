package com.side.hhplusecommerce.coupon.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "쿠폰 발급 응답")
public class IssueCouponResponse {
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

    @Schema(description = "만료일시", example = "2025-12-30T23:59:59")
    private LocalDateTime expiresAt;

    @Schema(description = "발급일시", example = "2025-10-30T12:00:00")
    private LocalDateTime issuedAt;

    public static IssueCouponResponse of(com.side.hhplusecommerce.coupon.domain.UserCoupon userCoupon, com.side.hhplusecommerce.coupon.domain.Coupon coupon) {
        return new IssueCouponResponse(
                userCoupon.getUserCouponId(),
                coupon.getCouponId(),
                coupon.getName(),
                coupon.getDiscountAmount(),
                userCoupon.getIsUsed(),
                coupon.getExpiresAt(),
                userCoupon.getIssuedAt()
        );
    }
}
