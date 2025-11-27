package com.side.hhplusecommerce.item.controller.dto;

import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.dto.ItemDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "상품 상세 조회 응답")
public class ItemResponse {
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

    @Schema(description = "인기 상품 여부", example = "true")
    private Boolean isPopular;

    public static ItemResponse from(Item item, boolean isPopular) {
        return new ItemResponse(
                item.getItemId(),
                item.getName(),
                item.getPrice(),
                item.getStock(),
                item.getCreatedAt(),
                isPopular
        );
    }

    public static ItemResponse from(ItemDto itemDto) {
        return new ItemResponse(
                itemDto.getItemId(),
                itemDto.getName(),
                itemDto.getPrice(),
                itemDto.getStock(),
                null,  // 인기 상품은 createdAt을 포함하지 않음
                true
        );
    }

    public static ItemResponse from(Item item) {
        return new ItemResponse(
                item.getItemId(),
                item.getName(),
                item.getPrice(),
                item.getStock(),
                item.getCreatedAt(),
                false
        );
    }
}
