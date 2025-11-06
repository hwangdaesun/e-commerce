package com.side.hhplusecommerce.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Item
    ITEM_NOT_FOUND("ITEM_000", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK("ITEM_001", "재고가 부족합니다.", HttpStatus.CONFLICT),
    INVALID_STOCK_QUANTITY("ITEM_002", "차감할 수량은 1 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_SALES_QUANTITY("ITEM_003", "판매 수량은 1 이상이어야 합니다.", HttpStatus.BAD_REQUEST),

    // Order
    INVALID_ORDER_AMOUNT("ORDER_001", "주문 금액은 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_COUPON_DISCOUNT("ORDER_002", "쿠폰 할인 금액은 0 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_ITEM_ORDER_ID("ORDER_003", "orderId는 필수입니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_ITEM_ITEM_ID("ORDER_004", "itemId는 필수입니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_ITEM_QUANTITY("ORDER_005", "수량은 1개 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_ITEM_PRICE("ORDER_006", "가격은 0원 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    ALREADY_PAID_ORDER("ORDER_007", "이미 결제가 완료된 주문입니다.", HttpStatus.BAD_REQUEST),

    // Coupon
    COUPON_NOT_FOUND("COUPON_000", "쿠폰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    COUPON_SOLD_OUT("COUPON_001", "쿠폰이 모두 소진되었습니다.", HttpStatus.CONFLICT),
    ALREADY_USED_COUPON("COUPON_002", "이미 사용된 쿠폰입니다.", HttpStatus.BAD_REQUEST),
    EXPIRED_COUPON("COUPON_003", "만료된 쿠폰입니다.", HttpStatus.BAD_REQUEST),
    ALREADY_ISSUED_COUPON("COUPON_004", "이미 발급받은 쿠폰입니다.", HttpStatus.CONFLICT),

    // Cart
    INVALID_CART_ITEM_QUANTITY("CART_001", "수량은 1개 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND("CART_002", "장바구니 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    NOT_OWNER_OF_CART_ITEM("CART_003", "본인의 장바구니가 아닙니다.", HttpStatus.FORBIDDEN),


    // Point
    INVALID_POINT_AMOUNT("Point_001", "포인트 금액은 1 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_POINT("Point_002", "포인트가 부족합니다.", HttpStatus.CONFLICT),
    USER_POINT_NOT_FOUND("POINT_003", "사용자 포인트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
