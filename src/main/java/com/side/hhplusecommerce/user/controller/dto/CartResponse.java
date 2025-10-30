package com.side.hhplusecommerce.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "장바구니 조회 응답")
public class CartResponse {
    @Schema(description = "장바구니 항목 목록")
    private List<CartItem> items;

    @Schema(description = "장바구니 요약 정보")
    private Summary summary;

    @Getter
    @AllArgsConstructor
    @Schema(description = "장바구니 항목 정보")
    public static class CartItem {
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
    }

    @Getter
    @AllArgsConstructor
    @Schema(description = "장바구니 요약 정보")
    public static class Summary {
        @Schema(description = "총 상품 종류 수", example = "2")
        private Integer totalItems;

        @Schema(description = "총 상품 수량", example = "3")
        private Integer totalQuantity;

        @Schema(description = "총 결제 금액", example = "117000")
        private Integer totalAmount;
    }
}