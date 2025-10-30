package com.side.hhplusecommerce.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "장바구니 상품 추가/수정 응답")
public class CartItemResponse {
    @Schema(description = "장바구니 항목 ID", example = "1")
    private Long cartItemId;

    @Schema(description = "상품 ID", example = "1")
    private Long itemId;

    @Schema(description = "상품명", example = "기본 티셔츠")
    private String itemName;

    @Schema(description = "상품 단가", example = "29000")
    private Integer price;

    @Schema(description = "수량", example = "2")
    private Integer quantity;

    @Schema(description = "총 가격 (단가 × 수량)", example = "58000")
    private Integer totalPrice;

    @Schema(description = "현재 재고 수량", example = "50")
    private Integer stock;

    @Schema(description = "장바구니 추가일시", example = "2025-10-30T12:00:00")
    private LocalDateTime createdAt;
}