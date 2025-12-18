package com.side.hhplusecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 데이터 플랫폼 전송 이벤트
 * 주문 완료 후 외부 시스템에 주문 데이터를 전송하기 위한 이벤트
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOrderDataEvent {
    private Long orderId;

    public static SendOrderDataEvent of(Long orderId) {
        return new SendOrderDataEvent(orderId);
    }
}