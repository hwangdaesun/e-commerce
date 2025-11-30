package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.common.lock.distributed.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 상품 재고 서비스 - DistributedLock 레이어
 * 분산락만 적용하고 트랜잭션은 내부 서비스에서 처리합니다.
 * 실행 순서: 락 획득 → 트랜잭션 시작 → 로직 실행 → 트랜잭션 커밋 → 락 해제
 */
@Service
@RequiredArgsConstructor
public class ItemStockService {
    private final ItemStockTransactionService itemStockTransactionService;

    /**
     * 개별 상품 재고 차감 (DistributedLock 적용)
     * 동일한 itemId에 대해 동시에 실행되지 않도록 분산락을 사용합니다.
     */
    @DistributedLock(keyResolver = "itemStockLockKeyResolver", key = "#itemId")
    public void decreaseStockForItem(Long itemId, int quantity) {
        itemStockTransactionService.decreaseStock(itemId, quantity);
    }

    /**
     * 개별 상품 재고 증가 (DistributedLock 적용)
     * 동일한 itemId에 대해 동시에 실행되지 않도록 분산락을 사용합니다.
     */
    @DistributedLock(keyResolver = "itemStockLockKeyResolver", key = "#itemId")
    public void increaseStockForItem(Long itemId, int quantity) {
        itemStockTransactionService.increaseStock(itemId, quantity);
    }
}
