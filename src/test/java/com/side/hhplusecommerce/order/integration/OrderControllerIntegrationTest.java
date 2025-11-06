package com.side.hhplusecommerce.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.hhplusecommerce.cart.domain.Cart;
import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.repository.CartItemRepository;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.repository.ItemRepository;
import com.side.hhplusecommerce.order.controller.dto.CreateOrderRequest;
import com.side.hhplusecommerce.point.domain.UserPoint;
import com.side.hhplusecommerce.point.repository.UserPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponStockRepository couponStockRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @BeforeEach
    void setUp() {
        // 모든 데이터 정리 (테스트 격리)
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        userCouponRepository.deleteAll();
        couponStockRepository.deleteAll();
        couponRepository.deleteAll();
        userPointRepository.deleteAll();
        itemRepository.deleteAll();

        // 상품 데이터
        Item item1 = Item.builder()
                .itemId(1L)
                .name("Test Item 1")
                .price(10000)
                .stock(100)
                .salesCount(0)
                .build();

        Item item2 = Item.builder()
                .itemId(2L)
                .name("Test Item 2")
                .price(20000)
                .stock(50)
                .salesCount(0)
                .build();

        Item item3 = Item.builder()
                .itemId(3L)
                .name("Test Item 3")
                .price(5000)
                .stock(5)
                .salesCount(0)
                .build();

        itemRepository.save(item1);
        itemRepository.save(item2);
        itemRepository.save(item3);

        // 장바구니 데이터
        Cart cart1 = Cart.builder()
                .cartId(1L)
                .userId(100L)
                .build();
        cartRepository.save(cart1);

        CartItem cartItem1 = CartItem.create(cart1.getCartId(), 1L, 2);
        CartItem cartItem2 = CartItem.create(cart1.getCartId(), 2L, 1);
        CartItem cartItem3 = CartItem.create(cart1.getCartId(), 3L, 10); // 재고 부족할 항목

        cartItemRepository.save(cartItem1);
        cartItemRepository.save(cartItem2);
        cartItemRepository.save(cartItem3);

        // 쿠폰 데이터
        Coupon coupon = Coupon.builder()
                .couponId(1L)
                .name("5000원 할인")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        Coupon expiredCoupon = Coupon.builder()
                .couponId(2L)
                .name("만료된 쿠폰")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        couponRepository.save(coupon);
        couponRepository.save(expiredCoupon);

        CouponStock couponStock1 = CouponStock.of(1L, 10);
        CouponStock couponStock2 = CouponStock.of(2L, 10);
        couponStockRepository.save(couponStock1);
        couponStockRepository.save(couponStock2);

        UserCoupon userCoupon1 = UserCoupon.issue(100L, 1L);
        UserCoupon userCoupon2 = UserCoupon.issue(100L, 2L);
        userCouponRepository.save(userCoupon1);
        userCouponRepository.save(userCoupon2);

        // 포인트 데이터
        UserPoint userPoint1 = UserPoint.initialize(100L);
        userPoint1.charge(100000);
        userPointRepository.save(userPoint1);

        UserPoint userPoint2 = UserPoint.initialize(200L);
        userPoint2.charge(1000); // 포인트 부족
        userPointRepository.save(userPoint2);
    }

    @Test
    @DisplayName("[성공] 주문 생성 - 쿠폰 없이")
    void createOrder_success_withoutCoupon() throws Exception {
        List<Long> cartItemIds = cartItemRepository.findByCartId(1L).stream()
                .filter(ci -> ci.getItemId() == 1L || ci.getItemId() == 2L)
                .map(CartItem::getCartItemId)
                .toList();

        CreateOrderRequest request = new CreateOrderRequest(100L, cartItemIds, null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("[성공] 주문 생성 - 쿠폰 사용")
    void createOrder_success_withCoupon() throws Exception {
        List<Long> cartItemIds = cartItemRepository.findByCartId(1L).stream()
                .filter(ci -> ci.getItemId() == 1L || ci.getItemId() == 2L)
                .map(CartItem::getCartItemId)
                .toList();

        UserCoupon userCoupon = userCouponRepository.findByUserId(100L).stream()
                .filter(uc -> uc.getCouponId() == 1L)
                .findFirst()
                .orElseThrow();

        CreateOrderRequest request = new CreateOrderRequest(100L, cartItemIds, userCoupon.getUserCouponId());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("[실패] 주문 생성 - 재고 부족")
    void createOrder_fail_insufficientStock() throws Exception {
        List<Long> cartItemIds = cartItemRepository.findByCartId(1L).stream()
                .filter(ci -> ci.getItemId() == 3L)
                .map(CartItem::getCartItemId)
                .toList();

        CreateOrderRequest request = new CreateOrderRequest(100L, cartItemIds, null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("[실패] 주문 생성 - 만료된 쿠폰 사용")
    void createOrder_fail_expiredCoupon() throws Exception {
        List<Long> cartItemIds = cartItemRepository.findByCartId(1L).stream()
                .filter(ci -> ci.getItemId() == 1L || ci.getItemId() == 2L)
                .map(CartItem::getCartItemId)
                .toList();

        UserCoupon expiredUserCoupon = userCouponRepository.findByUserId(100L).stream()
                .filter(uc -> uc.getCouponId() == 2L)
                .findFirst()
                .orElseThrow();

        CreateOrderRequest request = new CreateOrderRequest(100L, cartItemIds, expiredUserCoupon.getUserCouponId());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[실패] 주문 생성 - 장바구니 항목 없음")
    void createOrder_fail_emptyCartItems() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(100L, List.of(), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
