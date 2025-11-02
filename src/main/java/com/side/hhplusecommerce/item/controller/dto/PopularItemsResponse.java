package com.side.hhplusecommerce.item.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "인기 상품 목록 조회 응답")
public class PopularItemsResponse {
    @Schema(description = "인기 상품 목록 (판매량 기준)")
    private List<PopularItem> popularItems;

    @Getter
    @AllArgsConstructor
    @Schema(description = "인기 상품 정보")
    public static class PopularItem {
        @Schema(description = "순위", example = "1")
        private Integer rank;

        @Schema(description = "상품 ID", example = "1")
        private Long itemId;

        @Schema(description = "상품명", example = "기본 티셔츠")
        private String itemName;

        @Schema(description = "가격", example = "29000")
        private Integer price;

        @Schema(description = "재고 수량", example = "50")
        private Integer stock;
    }
}