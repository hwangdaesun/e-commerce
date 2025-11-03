package com.side.hhplusecommerce.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Item
    INSUFFICIENT_STOCK("ITEM_001", "재고가 부족합니다.", HttpStatus.CONFLICT),
    INVALID_STOCK_QUANTITY("ITEM_002", "차감할 수량은 1 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_ITEM_ORDER_ID("ORDER_003", "orderId는 필수입니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_ITEM_ITEM_ID("ORDER_004", "itemId는 필수입니다.", HttpStatus.BAD_REQUEST),

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}