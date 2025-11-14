package com.side.hhplusecommerce.order.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.hhplusecommerce.ContainerTest;
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
class OrderControllerIntegrationTest extends ContainerTest {

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

    private Long userWithSufficientPointsId;     // 포인트가 충분한 사용자 ID
    private Long userWithInsufficientPointsId;   // 포인트가 부족한 사용자 ID
    private Long validCouponId;                   // 유효한 쿠폰 ID
    private Long expiredCouponId;                 // 만료된 쿠폰 ID
    private Long normalItem1Id;                   // 재고 충분한 상품1 ID
    private Long normalItem2Id;                   // 재고 충분한 상품2 ID
    private Long lowStockItemId;                  // 재고 부족한 상품 ID
    private Long testCartId;                      // 테스트용 장바구니 ID

    @BeforeEach
    void setUp() {
        userWithSufficientPointsId = 100L;
        userWithInsufficientPointsId = 200L;

        // 상품 데이터
        Item item1 = Item.builder()
                .name("Test Item 1")
                .price(10000)
                .stock(100)
                .version(1L)
                .build();

        Item item2 = Item.builder()
                .name("Test Item 2")
                .price(20000)
                .stock(50)
                .version(1L)
                .build();

        Item item3 = Item.builder()
                .name("Test Item 3")
                .price(5000)
                .stock(5)
                .version(1L)
                .build();

        Item savedItem1 = itemRepository.save(item1);
        Item savedItem2 = itemRepository.save(item2);
        Item savedItem3 = itemRepository.save(item3);

        normalItem1Id = savedItem1.getItemId();
        normalItem2Id = savedItem2.getItemId();
        lowStockItemId = savedItem3.getItemId();

        // 장바구니 데이터
        Cart cart1 = Cart.builder()
                .userId(userWithSufficientPointsId)
                .build();
        Cart savedCart = cartRepository.save(cart1);
        testCartId = savedCart.getCartId();

        CartItem cartItem1 = CartItem.create(testCartId, normalItem1Id, 2);
        CartItem cartItem2 = CartItem.create(testCartId, normalItem2Id, 1);
        CartItem cartItem3 = CartItem.create(testCartId, lowStockItemId, 10); // 재고 부족할 항목

        cartItemRepository.save(cartItem1);
        cartItemRepository.save(cartItem2);
        cartItemRepository.save(cartItem3);

        // 쿠폰 데이터
        Coupon coupon = Coupon.builder()
                .name("5000원 할인")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        Coupon expiredCoupon = Coupon.builder()
                .name("만료된 쿠폰")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        Coupon savedCoupon = couponRepository.save(coupon);
        Coupon savedExpiredCoupon = couponRepository.save(expiredCoupon);

        validCouponId = savedCoupon.getCouponId();
        expiredCouponId = savedExpiredCoupon.getCouponId();

        CouponStock couponStock1 = CouponStock.of(validCouponId, 10);
        CouponStock couponStock2 = CouponStock.of(expiredCouponId, 10);
        couponStockRepository.save(couponStock1);
        couponStockRepository.save(couponStock2);

        UserCoupon userCoupon1 = UserCoupon.issue(userWithSufficientPointsId, validCouponId);
        UserCoupon userCoupon2 = UserCoupon.issue(userWithSufficientPointsId, expiredCouponId);
        userCouponRepository.save(userCoupon1);
        userCouponRepository.save(userCoupon2);

        // 포인트 데이터
        UserPoint userPoint1 = UserPoint.initialize(userWithSufficientPointsId);
        userPoint1.charge(100000);
        userPointRepository.save(userPoint1);

        UserPoint userPoint2 = UserPoint.initialize(userWithInsufficientPointsId);
        userPoint2.charge(1000); // 포인트 부족
        userPointRepository.save(userPoint2);
    }

    @Test
    @DisplayName("[성공] 주문 생성 - 쿠폰 없이")
    void createOrder_success_withoutCoupon() throws Exception {
        List<Long> cartItemIds = cartItemRepository.findByCartId(testCartId).stream()
                .filter(ci -> ci.getItemId().equals(normalItem1Id) || ci.getItemId().equals(normalItem2Id))
                .map(CartItem::getCartItemId)
                .toList();

        CreateOrderRequest request = new CreateOrderRequest(userWithSufficientPointsId, cartItemIds, null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("[성공] 주문 생성 - 쿠폰 사용")
    void createOrder_success_withCoupon() throws Exception {
        List<Long> cartItemIds = cartItemRepository.findByCartId(testCartId).stream()
                .filter(ci -> ci.getItemId().equals(normalItem1Id) || ci.getItemId().equals(normalItem2Id))
                .map(CartItem::getCartItemId)
                .toList();

        UserCoupon userCoupon = userCouponRepository.findByUserId(userWithSufficientPointsId).stream()
                .filter(uc -> uc.getCouponId().equals(validCouponId))
                .findFirst()
                .orElseThrow();

        CreateOrderRequest request = new CreateOrderRequest(userWithSufficientPointsId, cartItemIds, userCoupon.getUserCouponId());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("[실패] 주문 생성 - 재고 부족")
    void createOrder_fail_insufficientStock() throws Exception {
        List<Long> cartItemIds = cartItemRepository.findByCartId(testCartId).stream()
                .filter(ci -> ci.getItemId().equals(lowStockItemId))
                .map(CartItem::getCartItemId)
                .toList();

        CreateOrderRequest request = new CreateOrderRequest(userWithSufficientPointsId, cartItemIds, null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("[실패] 주문 생성 - 만료된 쿠폰 사용")
    void createOrder_fail_expiredCoupon() throws Exception {
        List<Long> cartItemIds = cartItemRepository.findByCartId(testCartId).stream()
                .filter(ci -> ci.getItemId().equals(normalItem1Id) || ci.getItemId().equals(normalItem2Id))
                .map(CartItem::getCartItemId)
                .toList();

        UserCoupon expiredUserCoupon = userCouponRepository.findByUserId(userWithSufficientPointsId).stream()
                .filter(uc -> uc.getCouponId().equals(expiredCouponId))
                .findFirst()
                .orElseThrow();

        CreateOrderRequest request = new CreateOrderRequest(userWithSufficientPointsId, cartItemIds, expiredUserCoupon.getUserCouponId());

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("[실패] 주문 생성 - 장바구니 항목 없음")
    void createOrder_fail_emptyCartItems() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(userWithSufficientPointsId, List.of(), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
