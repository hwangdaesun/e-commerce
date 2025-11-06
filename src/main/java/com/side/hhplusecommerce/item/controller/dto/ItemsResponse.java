package com.side.hhplusecommerce.item.controller.dto;

import com.side.hhplusecommerce.item.domain.Item;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "상품 목록 조회 응답")
public class ItemsResponse {
    @Schema(description = "상품 목록")
    private List<ItemInfo> items;

    @Schema(description = "다음 페이지 조회를 위한 커서 값 (마지막 상품 ID)", example = "20")
    private Long nextCursor;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private Boolean hasNext;

    @Getter
    @AllArgsConstructor
    @Schema(description = "상품 정보")
    public static class ItemInfo {
        @Schema(description = "상품 ID", example = "1")
        private Long itemId;

        @Schema(description = "상품명", example = "기본 티셔츠")
        private String name;

        @Schema(description = "가격", example = "29000")
        private Integer price;

        @Schema(description = "재고 수량", example = "50")
        private Integer stock;

        @Schema(description = "상품 등록일시", example = "2025-10-30T12:00:00")
        private LocalDateTime createdAt;

        public static ItemInfo from(Item item) {
            return new ItemInfo(
                    item.getItemId(),
                    item.getName(),
                    item.getPrice(),
                    item.getStock(),
                    item.getCreatedAt()
            );
        }
    }

    public static ItemsResponse of(List<ItemInfo> items, Long nextCursor, Boolean hasNext) {
        return new ItemsResponse(items, nextCursor, hasNext);
    }
}
