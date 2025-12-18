package com.side.hhplusecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensateCouponCommand {
    private Long orderId;
    private Long userCouponId;

    public static CompensateCouponCommand of(Long orderId, Long userCouponId) {
        return new CompensateCouponCommand(orderId, userCouponId);
    }
}