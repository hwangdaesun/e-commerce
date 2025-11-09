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
@Schema(description = "장바구니 수량 수정 요청")
public class UpdateCartItemRequest {
    @NotNull(message = "사용자 ID는 필수입니다")
    @Schema(description = "사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @NotNull(message = "수량은 필수입니다")
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    @Schema(description = "변경할 수량", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantity;
}
