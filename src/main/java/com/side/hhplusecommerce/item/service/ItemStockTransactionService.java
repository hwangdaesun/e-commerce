package com.side.hhplusecommerce.item.service;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 재고 트랜잭션 서비스 - 트랜잭션 레이어
 * 트랜잭션만 적용하고 락은 외부 서비스(ItemStockService)에서 처리합니다.
 * 분산락을 사용하므로 비관적 락은 제거했습니다.
 * REQUIRES_NEW를 사용하여 분산락 내에서 독립적인 트랜잭션으로 실행됩니다.
 */
@Service
@RequiredArgsConstructor
public class ItemStockTransactionService {
    private final ItemRepository itemRepository;

    /**
     * 재고 차감 (트랜잭션 처리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(Long itemId, int quantity) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        item.decrease(quantity);
        itemRepository.save(item);
    }

    /**
     * 재고 증가 (트랜잭션 처리)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increaseStock(Long itemId, int quantity) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        item.increase(quantity);
        itemRepository.save(item);
    }
}
