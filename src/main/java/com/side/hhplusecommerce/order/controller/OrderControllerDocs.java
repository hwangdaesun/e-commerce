package com.side.hhplusecommerce.order.controller;

import com.side.hhplusecommerce.order.controller.dto.CreateOrderRequest;
import com.side.hhplusecommerce.order.controller.dto.CreateOrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "주문", description = "주문 API")
public interface OrderControllerDocs {

    @Operation(summary = "주문 생성", description = "장바구니의 상품들로 주문을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "주문 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "장바구니 항목 없음"),
            @ApiResponse(responseCode = "409", description = "재고 부족 또는 잔액 부족"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest request);
}