package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.CartItemRequest;
import com.side.hhplusecommerce.user.controller.dto.CartItemResponse;
import com.side.hhplusecommerce.user.controller.dto.CartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "장바구니", description = "장바구니 API")
public interface CartControllerDocs {

    @Operation(summary = "장바구니 상품 추가", description = "장바구니에 상품을 추가합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "추가 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 상품"),
            @ApiResponse(responseCode = "409", description = "재고 부족"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<CartItemResponse> addCartItem(@RequestBody CartItemRequest request);

    @Operation(summary = "장바구니 조회", description = "현재 사용자의 장바구니 전체 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<CartResponse> getCart(
            @Parameter(description = "사용자 ID", required = true)
            Long userId
    );
}
