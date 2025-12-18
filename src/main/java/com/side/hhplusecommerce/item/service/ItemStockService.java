package com.side.hhplusecommerce.item.service;

import static com.side.hhplusecommerce.config.RedisCacheConfig.ITEM;
import static com.side.hhplusecommerce.config.RedisCacheConfig.LOW_STOCK_THRESHOLD;

import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.common.lock.distributed.DistributedLock;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.order.domain.OrderItem;
import com.side.hhplusecommerce.order.event.CompensateStockCommand;
import com.side.hhplusecommerce.order.event.OrderCreatedEvent;
import com.side.hhplusecommerce.order.event.StockFailedEvent;
import com.side.hhplusecommerce.order.event.StockReservedEvent;
import com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaProducer;
import com.side.hhplusecommerce.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.side.hhplusecommerce.order.infrastructure.kafka.OrderEventKafkaConstants.*;

/**
 * 상품 재고 서비스
 * @Order를 통해 분산락(Order=1)이 트랜잭션보다 먼저 적용됩니다.
 * 실행 순서: 락 획득 → 트랜잭션 시작 → 로직 실행 → 트랜잭션 커밋 → 락 해제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemStockService {
    private final ItemRepository itemRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderEventKafkaProducer kafkaProducer;
    private final CacheManager cacheManager;

    /**
     * 개별 상품 재고 차감
     * 분산락(Order=1)과 트랜잭션을 함께 사용합니다.
     * 실행 순서: 락 획득 → 트랜잭션 시작 → 재고 차감 → 트랜잭션 커밋 → 락 해제
     */
    @Transactional
    @DistributedLock(keyResolver = "itemStockLockKeyResolver", key = "#itemId")
    public void decreaseStockForItem(Long itemId, int quantity) {
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
     * 개별 상품 재고 증가
     * 분산락(Order=1)과 트랜잭션을 함께 사용합니다.
     * 실행 순서: 락 획득 → 트랜잭션 시작 → 재고 증가 → 트랜잭션 커밋 → 락 해제
     */
    @Transactional
    @DistributedLock(keyResolver = "itemStockLockKeyResolver", key = "#itemId")
    public void increaseStockForItem(Long itemId, int quantity) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new CustomException(ErrorCode.ITEM_NOT_FOUND));

        item.increase(quantity);
        itemRepository.save(item);
    }

    /**
     * OrderCreatedEvent 처리 - 재고 예약 처리
     */
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("ItemStockService received OrderCreatedEvent: orderId={}, itemIds={}",
                event.getOrderId(), event.getItemIds());

        try {
            // OrderItem에서 각 itemId에 대한 수량 조회
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(event.getOrderId());

            // 각 상품별로 재고 차감
            for (OrderItem orderItem : orderItems) {
                decreaseStockForItem(orderItem.getItemId(), orderItem.getQuantity());
                log.info("Stock decreased: itemId={}, quantity={}", orderItem.getItemId(), orderItem.getQuantity());
            }

            log.info("Stock reservation successful for orderId={}", event.getOrderId());
            StockReservedEvent stockReservedEvent = StockReservedEvent.of(event.getOrderId());
            kafkaProducer.publish(TOPIC_STOCK_RESERVED, event.getOrderId().toString(), stockReservedEvent);

        } catch (Exception e) {
            log.error("Stock reservation failed for orderId={}", event.getOrderId(), e);
            StockFailedEvent stockFailedEvent = StockFailedEvent.of(event.getOrderId(), e.getMessage());
            kafkaProducer.publish(TOPIC_STOCK_FAILED, event.getOrderId().toString(), stockFailedEvent);
        }
    }

    /**
     * CompensateStockCommand 처리 - 재고 복구 처리
     */
    public void handleCompensateStockCommand(CompensateStockCommand command) {
        log.info("ItemStockService received CompensateStockCommand: orderId={}, itemQuantities={}",
                command.getOrderId(), command.getItemQuantities());

        try {
            // 각 상품별로 재고 복구
            for (Map.Entry<Long, Integer> entry : command.getItemQuantities().entrySet()) {
                Long itemId = entry.getKey();
                Integer quantity = entry.getValue();
                increaseStockForItem(itemId, quantity);
                log.info("Stock compensated: itemId={}, quantity={}", itemId, quantity);
            }
            log.info("Stock compensation completed for orderId={}", command.getOrderId());

        } catch (Exception e) {
            log.error("Stock compensation failed for orderId={}", command.getOrderId(), e);
        }
    }

    /**
     * 상품 캐시 무효화
     * 재고가 임계값 이하로 떨어지거나 중요한 변경 사항이 있을 때 호출
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
