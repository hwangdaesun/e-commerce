package com.side.hhplusecommerce.order.service;

import com.side.hhplusecommerce.order.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExternalDataPlatformService {

    @Async
    public void sendOrderDataAsync(Order order) {
        log.info("외부 데이터 플랫폼으로 주문 정보 전송 시작 - orderId: {}", order.getOrderId());
        // 실제 외부 API 호출 로직이 여기에 들어갈 수 있음
        // 예: restTemplate.postForEntity(externalUrl, orderData, ResponseEntity.class);
        log.info("외부 데이터 플랫폼으로 주문 정보 전송 완료 - orderId: {}", order.getOrderId());
    }
}
