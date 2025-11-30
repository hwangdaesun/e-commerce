package com.side.hhplusecommerce.common.lock;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 락 키 타입 Enum
 * 락 키의 프리픽스를 중앙에서 관리합니다.
 * 분산락과 스핀락 모두에서 사용됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum LockKeyType {
    USER_POINT("user-point"),
    ORDER("order"),
    COUPON("coupon"),
    ITEM_STOCK("item-stock");

    private final String prefix;
}