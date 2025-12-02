package com.side.hhplusecommerce.item.integration;

import static com.side.hhplusecommerce.item.constants.PopularityConstants.POPULAR_ITEMS_DAILY_KEY;
import static com.side.hhplusecommerce.item.constants.PopularityConstants.POPULAR_ITEMS_WEEKLY_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemView;
import com.side.hhplusecommerce.item.dto.PopularItemsDto;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.item.repository.ItemViewRepository;
import com.side.hhplusecommerce.item.service.ItemPopularityService;
import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.domain.OrderItem;
import com.side.hhplusecommerce.order.repository.OrderItemRepository;
import com.side.hhplusecommerce.order.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class PopularItemsIntegrationTest extends ContainerTest {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemViewRepository itemViewRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ItemPopularityService itemPopularityService;


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private Item item1;
    private Item item2;
    private Item item3;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.delete(POPULAR_ITEMS_DAILY_KEY);
        redisTemplate.delete(POPULAR_ITEMS_WEEKLY_KEY);

        // 테스트 상품 생성
        item1 = Item.builder()
                .name("상품 1")
                .price(10000)
                .stock(100)
                .build();
        item1 = itemRepository.save(item1);

        item2 = Item.builder()
                .name("상품 2")
                .price(20000)
                .stock(100)
                .build();
        item2 = itemRepository.save(item2);

        item3 = Item.builder()
                .name("상품 3")
                .price(30000)
                .stock(100)
                .build();
        item3 = itemRepository.save(item3);
    }

    @AfterEach
    void tearDown() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        itemViewRepository.deleteAll();
        itemRepository.deleteAll();

        // Redis 정리
        redisTemplate.delete(POPULAR_ITEMS_DAILY_KEY);
        redisTemplate.delete(POPULAR_ITEMS_WEEKLY_KEY);
    }

    @Test
    @DisplayName("일간 인기 상품 랭킹 업데이트 및 조회 테스트")
    void updateAndGetDailyPopularItems() {
        // given - 최근 1일 내 조회수 및 판매 데이터 생성
        LocalDateTime now = LocalDateTime.now();

        // 상품1: 조회수 10회
        for (int i = 0; i < 10; i++) {
            ItemView view = ItemView.builder()
                    .itemId(item1.getItemId())
                    .userId((long) i)
                    .build();
            itemViewRepository.save(view);
        }

        // 상품2: 조회수 5회, 판매 2회
        for (int i = 0; i < 5; i++) {
            ItemView view = ItemView.builder()
                    .itemId(item2.getItemId())
                    .userId((long) i)
                    .build();
            itemViewRepository.save(view);
        }
        createOrderWithItems(item2, 2);

        // 상품3: 판매 3회
        createOrderWithItems(item3, 3);

        // when - 일간 인기 상품 랭킹 업데이트 (최근 1일 기준)
        LocalDateTime oneDayAgo = now.minusDays(1);
        itemPopularityService.updatePopularItemsRanking(POPULAR_ITEMS_DAILY_KEY, oneDayAgo);

        // then - Redis ZSET에 올바르게 저장되었는지 확인
        Set<Object> dailyRanking = redisTemplate.opsForZSet()
                .reverseRange(POPULAR_ITEMS_DAILY_KEY, 0, -1);

        assertThat(dailyRanking).isNotNull();
        assertThat(dailyRanking).hasSize(3);

        // 인기도 순서 확인: 상품2(조회5+판매2=26점) > 상품3(판매3=24점) > 상품1(조회10=20점)
        Object[] items = dailyRanking.toArray();
        assertThat(items[0].toString()).isEqualTo(item2.getItemId().toString());
        assertThat(items[1].toString()).isEqualTo(item3.getItemId().toString());
        assertThat(items[2].toString()).isEqualTo(item1.getItemId().toString());
    }

    @Test
    @DisplayName("주간 인기 상품 랭킹 업데이트 및 조회 테스트")
    void updateAndGetWeeklyPopularItems() {
        // given - 최근 7일 내 조회수 및 판매 데이터 생성
        LocalDateTime now = LocalDateTime.now();

        // 상품1: 조회수 20회
        for (int i = 0; i < 20; i++) {
            ItemView view = ItemView.builder()
                    .itemId(item1.getItemId())
                    .userId((long) i)
                    .build();
            itemViewRepository.save(view);
        }

        // 상품2: 조회수 10회, 판매 5회
        for (int i = 0; i < 10; i++) {
            ItemView view = ItemView.builder()
                    .itemId(item2.getItemId())
                    .userId((long) i)
                    .build();
            itemViewRepository.save(view);
        }
        createOrderWithItems(item2, 5);

        // 상품3: 판매 8회
        createOrderWithItems(item3, 8);

        // when - 주간 인기 상품 랭킹 업데이트 (최근 7일 기준)
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        itemPopularityService.updatePopularItemsRanking(POPULAR_ITEMS_WEEKLY_KEY, sevenDaysAgo);

        // then - Redis ZSET에 올바르게 저장되었는지 확인
        Set<Object> weeklyRanking = redisTemplate.opsForZSet()
                .reverseRange(POPULAR_ITEMS_WEEKLY_KEY, 0, -1);

        assertThat(weeklyRanking).isNotNull();
        assertThat(weeklyRanking.size()).isEqualTo(3);

        // 인기도 순서 확인: 상품3(판매8=64점) > 상품2(조회10+판매5=60점) > 상품1(조회20=40점)
        Object[] items = weeklyRanking.toArray();
        assertThat(items[0].toString()).isEqualTo(item3.getItemId().toString());
        assertThat(items[1].toString()).isEqualTo(item2.getItemId().toString());
        assertThat(items[2].toString()).isEqualTo(item1.getItemId().toString());
    }

    @Test
    @DisplayName("조회 시 일간/주간 ZSET score 동시 증가 테스트")
    void incrementViewScoreForBothKeys() {
        // given - 초기 랭킹 데이터 생성
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        // 상품1에 대한 초기 조회 데이터 생성
        for (int i = 0; i < 5; i++) {
            ItemView view = ItemView.builder()
                    .itemId(item1.getItemId())
                    .userId((long) i)
                    .build();
            itemViewRepository.save(view);
        }

        itemPopularityService.updatePopularItemsRanking(POPULAR_ITEMS_DAILY_KEY, oneDayAgo);
        itemPopularityService.updatePopularItemsRanking(POPULAR_ITEMS_WEEKLY_KEY, sevenDaysAgo);

        // 초기 score 확인
        Double initialDailyScore = redisTemplate.opsForZSet()
                .score(POPULAR_ITEMS_DAILY_KEY, item1.getItemId().toString());
        Double initialWeeklyScore = redisTemplate.opsForZSet()
                .score(POPULAR_ITEMS_WEEKLY_KEY, item1.getItemId().toString());

        // when - 조회 시 score 증가
        itemPopularityService.incrementViewScore(item1.getItemId());

        // then - 양쪽 ZSET 모두 score가 VIEW_SCORE_WEIGHT(2)만큼 증가
        Double updatedDailyScore = redisTemplate.opsForZSet()
                .score(POPULAR_ITEMS_DAILY_KEY, item1.getItemId().toString());
        Double updatedWeeklyScore = redisTemplate.opsForZSet()
                .score(POPULAR_ITEMS_WEEKLY_KEY, item1.getItemId().toString());

        assertThat(updatedDailyScore).isEqualTo(initialDailyScore + 2);
        assertThat(updatedWeeklyScore).isEqualTo(initialWeeklyScore + 2);
    }

    @Test
    @DisplayName("판매 시 일간/주간 ZSET score 동시 증가 테스트")
    void incrementSalesScoreForBothKeys() {
        // given - 초기 랭킹 데이터 생성
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        createOrderWithItems(item1, 1);

        itemPopularityService.updatePopularItemsRanking(POPULAR_ITEMS_DAILY_KEY, oneDayAgo);
        itemPopularityService.updatePopularItemsRanking(POPULAR_ITEMS_WEEKLY_KEY, sevenDaysAgo);

        // 초기 score 확인
        Double initialDailyScore = redisTemplate.opsForZSet()
                .score(POPULAR_ITEMS_DAILY_KEY, item1.getItemId().toString());
        Double initialWeeklyScore = redisTemplate.opsForZSet()
                .score(POPULAR_ITEMS_WEEKLY_KEY, item1.getItemId().toString());

        // when - 판매 시 score 증가
        itemPopularityService.incrementSalesScore(item1.getItemId());

        // then - 양쪽 ZSET 모두 score가 SALES_SCORE_WEIGHT(8)만큼 증가
        Double updatedDailyScore = redisTemplate.opsForZSet()
                .score(POPULAR_ITEMS_DAILY_KEY, item1.getItemId().toString());
        Double updatedWeeklyScore = redisTemplate.opsForZSet()
                .score(POPULAR_ITEMS_WEEKLY_KEY, item1.getItemId().toString());

        assertThat(updatedDailyScore).isEqualTo(initialDailyScore + 8);
        assertThat(updatedWeeklyScore).isEqualTo(initialWeeklyScore + 8);
    }

    @Test
    @DisplayName("ZSET에 없는 상품의 score 증가 시도 시 에러 없이 무시")
    void incrementScoreForNonExistentItem() {
        // given - Redis ZSET이 비어있는 상태

        // when & then - 존재하지 않는 상품의 score 증가 시도 (에러 발생하지 않아야 함)
        itemPopularityService.incrementViewScore(999L);
        itemPopularityService.incrementSalesScore(999L);

        // ZSET은 여전히 비어있어야 함
        Long dailySize = redisTemplate.opsForZSet().size(POPULAR_ITEMS_DAILY_KEY);
        Long weeklySize = redisTemplate.opsForZSet().size(POPULAR_ITEMS_WEEKLY_KEY);

        assertThat(dailySize).isEqualTo(0L);
        assertThat(weeklySize).isEqualTo(0L);
    }

    /**
     * 테스트용 주문 및 주문 아이템 생성
     */
    private void createOrderWithItems(Item item, int quantity) {
        // Order 생성
        Order order = Order.create(null, 1L, item.getPrice() * quantity, 0);
        order = orderRepository.save(order);

        // 결제 처리
        order.completePay();
        orderRepository.save(order);

        // OrderItem 생성
        for (int i = 0; i < quantity; i++) {
            OrderItem orderItem = OrderItem.create(
                    order.getOrderId(),
                    item.getItemId(),
                    item.getName(),
                    item.getPrice(),
                    1,
                    null
            );
            orderItemRepository.save(orderItem);
        }
    }
}