package com.side.hhplusecommerce.item.service;

import static com.side.hhplusecommerce.config.RedisCacheConfig.ITEM;
import static com.side.hhplusecommerce.config.RedisCacheConfig.LOW_STOCK_THRESHOLD;
import static org.assertj.core.api.Assertions.assertThat;

import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.dto.ItemDto;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ItemPopularityServiceCacheTest {

    @Autowired
    private ItemPopularityService itemPopularityService;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CacheManager cacheManager;

    private Item highStockItem;
    private Item lowStockItem;

    @BeforeEach
    void setUp() {
        // 캐시 초기화
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        // 테스트 데이터 생성
        // 재고가 충분한 상품 (임계값 초과)
        highStockItem = Item.builder()
                .name("재고 충분한 상품")
                .price(10000)
                .stock(100)  // LOW_STOCK_THRESHOLD(10)보다 훨씬 큼
                .build();
        highStockItem = itemRepository.save(highStockItem);

        // 재고가 부족한 상품 (임계값 이하)
        lowStockItem = Item.builder()
                .name("재고 부족한 상품")
                .price(20000)
                .stock(5)  // LOW_STOCK_THRESHOLD(10) 이하
                .build();
        lowStockItem = itemRepository.save(lowStockItem);
    }

    @AfterEach
    void tearDown() {
        itemRepository.deleteAll();

        // 캐시 초기화
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Test
    @DisplayName("재고가 임계값 초과인 상품은 캐싱된다")
    void highStockItem_shouldBeCached() {
        // given
        Long itemId = highStockItem.getItemId();
        Cache cache = cacheManager.getCache(ITEM);
        assertThat(cache).isNotNull();

        // when - 첫 번째 조회 (DB 조회 후 캐싱)
        ItemDto result1 = itemPopularityService.getItemV1(itemId);

        // then - 캐시에 저장되었는지 확인
        Cache.ValueWrapper cachedValue = cache.get(itemId);
        assertThat(cachedValue).isNotNull();
        assertThat(cachedValue.get()).isInstanceOf(ItemDto.class);

        ItemDto cachedDto = (ItemDto) cachedValue.get();
        assertThat(cachedDto.getItemId()).isEqualTo(itemId);
        assertThat(cachedDto.getStock()).isEqualTo(100);
        assertThat(result1.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("재고가 임계값 이하인 상품은 캐싱되지 않는다")
    void lowStockItem_shouldNotBeCached() {
        // given
        Long itemId = lowStockItem.getItemId();
        Cache cache = cacheManager.getCache(ITEM);
        assertThat(cache).isNotNull();

        // when - 첫 번째 조회 (DB 조회, 캐싱 안 됨)
        ItemDto result1 = itemPopularityService.getItemV1(itemId);

        // then - 캐시에 저장되지 않았는지 확인
        Cache.ValueWrapper cachedValue = cache.get(itemId);
        assertThat(cachedValue).isNull();  // 캐싱되지 않음
        assertThat(result1.getStock()).isEqualTo(5);
        assertThat(result1.getStock()).isLessThanOrEqualTo(LOW_STOCK_THRESHOLD);
    }

    @Test
    @DisplayName("재고가 임계값(10)인 상품도 캐싱되지 않는다")
    void thresholdStockItem_shouldNotBeCached() {
        // given - 재고가 정확히 임계값인 상품
        Item thresholdItem = Item.builder()
                .name("재고 임계값 상품")
                .price(15000)
                .stock(LOW_STOCK_THRESHOLD)  // 정확히 10
                .build();
        thresholdItem = itemRepository.save(thresholdItem);
        Long itemId = thresholdItem.getItemId();

        Cache cache = cacheManager.getCache(ITEM);
        assertThat(cache).isNotNull();

        // when
        ItemDto result = itemPopularityService.getItemV1(itemId);

        // then - 임계값과 같을 때도 캐싱 안 됨 (<=  조건)
        Cache.ValueWrapper cachedValue = cache.get(itemId);
        assertThat(cachedValue).isNull();
        assertThat(result.getStock()).isEqualTo(LOW_STOCK_THRESHOLD);
    }
    
}