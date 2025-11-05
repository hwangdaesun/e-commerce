package com.side.hhplusecommerce.cart.controller.dto;

import com.side.hhplusecommerce.item.domain.Item;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

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

    public static CartResponse of(List<com.side.hhplusecommerce.cart.domain.CartItem> cartItems, Map<Long, Item> itemMap) {
        List<CartItem> items = cartItems.stream()
                .map(cartItem -> {
                    Item item = itemMap.get(cartItem.getItemId());
                    return new CartItem(
                            cartItem.getCartItemId(),
                            item.getItemId(),
                            item.getName(),
                            item.getPrice(),
                            cartItem.getQuantity(),
                            cartItem.calculateTotalPrice(item.getPrice()),
                            item.getStock()
                    );
                })
                .toList();

        Integer totalItems = items.size();
        Integer totalQuantity = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        Integer totalAmount = items.stream()
                .mapToInt(CartItem::getTotalPrice)
                .sum();

        Summary summary = new Summary(totalItems, totalQuantity, totalAmount);
        return new CartResponse(items, summary);
    }
}
