package com.side.hhplusecommerce.order.controller;

import com.side.hhplusecommerce.order.controller.dto.CreateOrderRequest;
import com.side.hhplusecommerce.order.controller.dto.CreateOrderResponse;
import com.side.hhplusecommerce.order.usecase.OrderCreateUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController implements OrderControllerDocs {
    private final OrderCreateUseCase orderCreateUseCase;

    @Override
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderCreateUseCase.create(
                request.getUserId(),
                request.getCartItemIds(),
                request.getUserCouponId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}