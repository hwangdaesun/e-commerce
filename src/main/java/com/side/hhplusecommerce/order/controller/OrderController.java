package com.side.hhplusecommerce.order.controller;

import com.side.hhplusecommerce.order.controller.dto.CreateOrderRequest;
import com.side.hhplusecommerce.order.controller.dto.CreateOrderResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController implements OrderControllerDocs {

    @Override
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        // Mock 데이터
        List<CreateOrderResponse.OrderItem> orderItems = List.of(
                new CreateOrderResponse.OrderItem(1L, 1L, "기본 티셔츠", 29000, 2, 58000),
                new CreateOrderResponse.OrderItem(2L, 2L, "청바지", 59000, 1, 59000)
        );

        CreateOrderResponse.CouponUsed couponUsed = new CreateOrderResponse.CouponUsed(
                1L,
                "신규 가입 쿠폰",
                5000
        );

        CreateOrderResponse response = new CreateOrderResponse(
                1L,
                orderItems,
                117000,
                5000,
                112000,
                112000,
                couponUsed,
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}