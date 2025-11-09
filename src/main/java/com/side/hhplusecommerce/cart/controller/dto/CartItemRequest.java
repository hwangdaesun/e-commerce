package com.side.hhplusecommerce.cart.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장바구니 상품 추가 요청")
public class CartItemRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "상품 ID는 필수입니다")
    @Schema(description = "상품 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long itemId;

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    @Schema(description = "수량", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}
