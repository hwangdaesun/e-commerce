package com.side.hhplusecommerce.order.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.domain.CartItemValidator;
import com.side.hhplusecommerce.cart.service.CartItemService;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.service.CouponService;
import com.side.hhplusecommerce.coupon.service.dto.CouponUseResult;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemValidator;
import com.side.hhplusecommerce.item.exception.InsufficientStockException;
import com.side.hhplusecommerce.item.service.ItemStockService;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.service.ExternalDataPlatformService;
import com.side.hhplusecommerce.order.service.OrderPaymentService;
import com.side.hhplusecommerce.order.service.OrderService;
import com.side.hhplusecommerce.order.service.dto.OrderCreateResult;
import com.side.hhplusecommerce.payment.service.UserPointService;
import com.side.hhplusecommerce.point.exception.InsufficientPointException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCreateUseCaseTest {

    @Mock
    private CartItemValidator cartItemValidator;

    @Mock
    private CartItemService cartItemService;

    @Mock
    private ItemValidator itemValidator;

    @Mock
    private ItemStockService itemStockService;

    @Mock
    private CouponService couponService;

    @Mock
    private OrderService orderService;

    @Mock
    private OrderPaymentService orderPaymentService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ExternalDataPlatformService externalDataPlatformService;

    @Mock
    private UserPointService userPointService;

    @InjectMocks
    private OrderCreateUseCase orderCreateUseCase;

    private Long userId;
    private List<Long> cartItemIds;
    private Long userCouponId;
    private List<CartItem> validCartItems;
    private List<Item> items;
    private Coupon coupon;
    private CouponUseResult couponUseResult;

    @BeforeEach
    void setUp() {
        userId = 1L;
        cartItemIds = List.of(1L, 2L);
        userCouponId = 100L;

        validCartItems = List.of(
                CartItem.createWithId(1L, 1L, 10L, 2),
                CartItem.createWithId(2L, 1L, 20L, 1)
        );

        items = List.of(
                Item.builder()
                        .itemId(10L)
                        .name("상품1")
                        .price(10000)
                        .stock(10)
                        .build(),
                Item.builder()
                        .itemId(20L)
                        .name("상품2")
                        .price(20000)
                        .stock(5)
                        .build()
        );

        coupon = null; // Mock으로 처리

        couponUseResult = new CouponUseResult(coupon, 5000);
    }

    @Test
    @DisplayName("재고 차감 실패 시 예외가 발생한다")
    void rollbackCouponOnStockDecreaseFail() {
        // given
        Order mockOrder = mock(Order.class);
        OrderCreateResult orderCreateResult = new OrderCreateResult(mockOrder, List.of());

        when(cartItemValidator.validateOwnership(userId, cartItemIds)).thenReturn(validCartItems);
        when(itemValidator.validateExistence(anyList())).thenReturn(items);
        when(couponService.useCoupon(userCouponId)).thenReturn(couponUseResult);
        when(cartItemService.calculateTotalAmount(validCartItems, items)).thenReturn(30000);
        when(orderService.createOrder(anyLong(), anyList(), anyList(), any(), any(), any()))
                .thenReturn(orderCreateResult);
        doThrow(new InsufficientStockException()).when(itemStockService).decreaseStock(validCartItems, items);

        // when & then
        assertThatThrownBy(() -> orderCreateUseCase.create(userId, cartItemIds, userCouponId))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("재고 차감 실패 시 쿠폰을 사용하지 않아도 예외가 발생한다")
    void noRollbackWhenNoCouponUsedOnStockDecreaseFail() {
        // given - userCouponId를 null로 설정
        Order mockOrder = mock(Order.class);
        OrderCreateResult orderCreateResult = new OrderCreateResult(mockOrder, List.of());

        when(cartItemValidator.validateOwnership(userId, cartItemIds)).thenReturn(validCartItems);
        when(itemValidator.validateExistence(anyList())).thenReturn(items);
        when(cartItemService.calculateTotalAmount(validCartItems, items)).thenReturn(30000);
        when(orderService.createOrder(anyLong(), anyList(), anyList(), any(), any(), any()))
                .thenReturn(orderCreateResult);
        doThrow(new InsufficientStockException()).when(itemStockService).decreaseStock(validCartItems, items);

        // when & then
        assertThatThrownBy(() -> orderCreateUseCase.create(userId, cartItemIds, null))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("주문 생성 실패 시 예외가 발생한다")
    void rollbackCouponAndStockOnOrderCreationFail() {
        // given
        when(cartItemValidator.validateOwnership(userId, cartItemIds)).thenReturn(validCartItems);
        when(itemValidator.validateExistence(anyList())).thenReturn(items);
        when(couponService.useCoupon(userCouponId)).thenReturn(couponUseResult);
        when(cartItemService.calculateTotalAmount(validCartItems, items)).thenReturn(30000);
        when(orderService.createOrder(anyLong(), anyList(), anyList(), any(), any(), any()))
                .thenThrow(new RuntimeException("주문 생성 실패"));

        // when & then
        assertThatThrownBy(() -> orderCreateUseCase.create(userId, cartItemIds, userCouponId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("주문 생성 실패");
    }

    @Test
    @DisplayName("주문 생성 실패 시 쿠폰을 사용하지 않아도 예외가 발생한다")
    void rollbackStockOnOrderCreationFailWithoutCoupon() {
        // given
        when(cartItemValidator.validateOwnership(userId, cartItemIds)).thenReturn(validCartItems);
        when(itemValidator.validateExistence(anyList())).thenReturn(items);
        when(cartItemService.calculateTotalAmount(validCartItems, items)).thenReturn(30000);
        when(orderService.createOrder(anyLong(), anyList(), anyList(), any(), any(), any()))
                .thenThrow(new RuntimeException("주문 생성 실패"));

        // when & then
        assertThatThrownBy(() -> orderCreateUseCase.create(userId, cartItemIds, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("주문 생성 실패");
    }

    @Test
    @DisplayName("결제 실패 시 예외가 발생한다")
    void rollbackCouponAndStockOnPaymentFail() {
        // given
        Order mockOrder = mock(Order.class);
        when(mockOrder.getFinalAmount()).thenReturn(25000);
        OrderCreateResult orderCreateResult = new OrderCreateResult(mockOrder, List.of());

        when(cartItemValidator.validateOwnership(userId, cartItemIds)).thenReturn(validCartItems);
        when(itemValidator.validateExistence(anyList())).thenReturn(items);
        when(couponService.useCoupon(userCouponId)).thenReturn(couponUseResult);
        when(cartItemService.calculateTotalAmount(validCartItems, items)).thenReturn(30000);
        when(orderService.createOrder(anyLong(), anyList(), anyList(), any(), any(), any()))
                .thenReturn(orderCreateResult);
        doThrow(new InsufficientPointException()).when(userPointService)
                .use(userId, 25000);

        // when & then
        assertThatThrownBy(() -> orderCreateUseCase.create(userId, cartItemIds, userCouponId))
                .isInstanceOf(InsufficientPointException.class);
    }

    @Test
    @DisplayName("결제 실패 시 쿠폰을 사용하지 않아도 예외가 발생한다")
    void rollbackStockOnPaymentFailWithoutCoupon() {
        // given
        Order mockOrder = mock(Order.class);
        when(mockOrder.getFinalAmount()).thenReturn(30000);
        OrderCreateResult orderCreateResult = new OrderCreateResult(mockOrder, List.of());

        when(cartItemValidator.validateOwnership(userId, cartItemIds)).thenReturn(validCartItems);
        when(itemValidator.validateExistence(anyList())).thenReturn(items);
        when(cartItemService.calculateTotalAmount(validCartItems, items)).thenReturn(30000);
        when(orderService.createOrder(anyLong(), anyList(), anyList(), any(), any(), any()))
                .thenReturn(orderCreateResult);
        doThrow(new InsufficientPointException()).when(userPointService)
                .use(userId, 30000);

        // when & then
        assertThatThrownBy(() -> orderCreateUseCase.create(userId, cartItemIds, null))
                .isInstanceOf(InsufficientPointException.class);
    }
}