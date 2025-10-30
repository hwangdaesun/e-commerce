package com.side.hhplusecommerce.user.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserCouponsResponse {
    private List<UserCoupon> coupons;

    @Getter
    @AllArgsConstructor
    public static class UserCoupon {
        private Long userCouponId;
        private Long couponId;
        private String couponName;
        private Integer discountAmount;
        private Boolean isUsed;
        private LocalDateTime usedAt;
        private LocalDateTime expiresAt;
        private LocalDateTime issuedAt;
    }
}