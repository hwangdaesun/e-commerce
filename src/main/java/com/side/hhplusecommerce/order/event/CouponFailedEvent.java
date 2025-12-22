package com.side.hhplusecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponFailedEvent {
    private Long orderId;
    private String reason;

    public static CouponFailedEvent of(Long orderId, String reason) {
        return new CouponFailedEvent(orderId, reason);
    }
}