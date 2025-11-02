package com.side.hhplusecommerce.item.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "상품 재고 조회 응답")
public class ItemStockResponse {
    @Schema(description = "상품 ID", example = "1")
    private Long itemId;

    @Schema(description = "상품명", example = "기본 티셔츠")
    private String itemName;

    @Schema(description = "재고 수량", example = "50")
    private Integer stock;
}