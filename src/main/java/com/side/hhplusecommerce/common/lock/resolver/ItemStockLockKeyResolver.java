package com.side.hhplusecommerce.common.lock.resolver;

import com.side.hhplusecommerce.common.lock.LockKeyResolver;
import com.side.hhplusecommerce.common.lock.LockKeyType;
import org.springframework.stereotype.Component;

/**
 * 상품 재고 락 키 생성 Resolver
 * 재고 차감/증가 시 itemId 기반으로 락 키를 생성합니다.
 */
@Component("itemStockLockKeyResolver")
public class ItemStockLockKeyResolver implements LockKeyResolver {

    @Override
    public String getKeyPrefix() {
        return LockKeyType.ITEM_STOCK.getPrefix();
    }
}
