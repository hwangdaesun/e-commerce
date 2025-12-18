package com.side.hhplusecommerce.coupon.infrastructure.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueMessage {
    private Long userId;
    private Long couponId;
    private LocalDateTime requestTime;

    public static CouponIssueMessage of(Long userId, Long couponId) {
        return CouponIssueMessage.builder()
                .userId(userId)
                .couponId(couponId)
                .requestTime(LocalDateTime.now())
                .build();
    }
}
