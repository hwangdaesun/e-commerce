package com.side.hhplusecommerce.user.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class IssueCouponResponse {
    private Long userCouponId;
    private Long couponId;
    private String couponName;
    private Integer discountAmount;
    private Boolean isUsed;
    private LocalDateTime expiresAt;
    private LocalDateTime issuedAt;
}