package com.side.hhplusecommerce.order.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.cart.domain.Cart;
import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.repository.CartItemRepository;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.order.controller.dto.CreateOrderResponse;
import com.side.hhplusecommerce.order.usecase.OrderCreateUseCase;
import com.side.hhplusecommerce.point.domain.UserPoint;
import com.side.hhplusecommerce.point.repository.UserPointRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 주문 생성 통합 테스트
 *
 * 주문 생성 API를 호출하고 즉시 응답을 검증합니다.
 * 비동기 이벤트 플로우는 별도의 E2E 테스트나 수동 테스트로 검증합니다.
 *
 * 테스트 시나리오:
 * 1. 정상 플로우: 재고 충분 + 쿠폰 사용 → 주문 생성 (PENDING)
 * 2. 정상 플로우: 재고 충분 + 쿠폰 없음 → 주문 생성 (PENDING)
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderCreateIntegrationTest extends ContainerTest {

    @Autowired
    private OrderCreateUseCase orderCreateUseCase;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    private Long userId;
    private Item item1;
    private Item item2;
    private Cart cart;
    private Coupon coupon;
    private UserCoupon userCoupon;

    @BeforeEach
    void setUp() {
        // 1. 사용자 ID 설정 (테스트용 고정값)
        userId = 1L;

        // 2. 상품 생성
        item1 = Item.builder()
                .name("상품1")
                .price(10000)
                .stock(100)
                .build();
        item1 = itemRepository.save(item1);

        item2 = Item.builder()
                .name("상품2")
                .price(20000)
                .stock(50)
                .build();
        item2 = itemRepository.save(item2);

        // 3. 장바구니 생성
        cart = Cart.create(userId);
        cart = cartRepository.save(cart);

        CartItem cartItem1 = CartItem.create(cart.getCartId(), item1.getItemId(), 2);
        CartItem cartItem2 = CartItem.create(cart.getCartId(), item2.getItemId(), 1);
        cartItemRepository.saveAll(List.of(cartItem1, cartItem2));

        // 4. 쿠폰 생성
        coupon = Coupon.builder()
                .name("5000원 할인")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        coupon = couponRepository.save(coupon);

        userCoupon = UserCoupon.issue(userId, coupon.getCouponId());
        userCoupon = userCouponRepository.save(userCoupon);

        // 5. 포인트 충전
        UserPoint userPoint = UserPoint.initialize(userId);
        userPoint.charge(100000);
        userPointRepository.save(userPoint);
    }

    @Test
    @DisplayName("[성공] 재고 충분 + 쿠폰 사용 → 주문 생성 (PENDING)")
    void successfulOrderWithCoupon() {
        // given
        List<Long> cartItemIds = cartItemRepository.findByCartId(cart.getCartId())
                .stream()
                .map(CartItem::getCartItemId)
                .toList();

        // when - 주문 생성 API 호출
        CreateOrderResponse response = orderCreateUseCase.create(
                userId,
                cartItemIds,
                userCoupon.getUserCouponId()
        );

        // then - 즉시 응답 확인 (주문이 PENDING 상태로 생성됨)
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getTotalAmount()).isEqualTo(40000); // 10000*2 + 20000*1
        assertThat(response.getCouponDiscount()).isEqualTo(5000);
        assertThat(response.getFinalAmount()).isEqualTo(35000);
    }

    @Test
    @DisplayName("[성공] 재고 충분 + 쿠폰 없음 → 주문 생성 (PENDING)")
    void successfulOrderWithoutCoupon() {
        // given
        List<Long> cartItemIds = cartItemRepository.findByCartId(cart.getCartId())
                .stream()
                .map(CartItem::getCartItemId)
                .toList();

        // when - 쿠폰 없이 주문 생성
        CreateOrderResponse response = orderCreateUseCase.create(
                userId,
                cartItemIds,
                null // 쿠폰 없음
        );

        // then - 즉시 응답 확인 (주문이 PENDING 상태로 생성됨)
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isNotNull();
        assertThat(response.getCouponDiscount()).isZero();
        assertThat(response.getFinalAmount()).isEqualTo(40000);
    }
}
