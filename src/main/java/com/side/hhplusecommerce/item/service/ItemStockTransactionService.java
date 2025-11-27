package com.side.hhplusecommerce.item.service;

import static com.side.hhplusecommerce.config.RedisCacheConfig.ITEM;
import static com.side.hhplusecommerce.config.RedisCacheConfig.LOW_STOCK_THRESHOLD;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 재고 트랜잭션 서비스 - 트랜잭션 레이어
 * 트랜잭션만 적용하고 락은 외부 서비스(ItemStockService)에서 처리합니다.
 * 분산락을 사용하므로 비관적 락은 제거했습니다.
 * REQUIRES_NEW를 사용하여 분산락 내에서 독립적인 트랜잭션으로 실행됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemStockTransactionService {
    private final ItemRepository itemRepository;
    private final CacheManager cacheManager;

    /**
     * 재고 차감 (트랜잭션 처리)
     * 재고가 임계값 이하로 떨어지면 캐시 무효화
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseStock(Long itemId, int quantity) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        item.decrease(quantity);
        Item savedItem = itemRepository.save(item);

        // 재고가 임계값 이하로 떨어진 경우 캐시 무효화
        // 낮은 재고 상태에서는 실시간 정확도가 중요하므로 캐싱하지 않음
        if (savedItem.getStock() <= LOW_STOCK_THRESHOLD) {
            evictItemCache(itemId);
            log.info("Cache evicted for low-stock item: itemId={}, stock={}",
                    itemId, savedItem.getStock());
        }
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

    /**
     * 상품 캐시 무효화
     * 재고가 0이 되거나 중요한 변경 사항이 있을 때 호출
     */
    private void evictItemCache(Long itemId) {
        try {
            var cache = cacheManager.getCache(ITEM);
            if (cache != null) {
                cache.evict(itemId);
                log.debug("Successfully evicted cache for itemId={}", itemId);
            }
        } catch (Exception e) {
            log.warn("Failed to evict cache for itemId={}", itemId, e);
            // 캐시 무효화 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }
}
