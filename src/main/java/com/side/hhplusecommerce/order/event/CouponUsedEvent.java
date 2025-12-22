package com.side.hhplusecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsedEvent {
    private Long orderId;

    public static CouponUsedEvent of(Long orderId) {
        return new CouponUsedEvent(orderId);
    }
}