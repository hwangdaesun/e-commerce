package com.side.hhplusecommerce.order.usecase;

import com.side.hhplusecommerce.cart.domain.Cart;
import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.domain.CartItemValidator;
import com.side.hhplusecommerce.cart.repository.CartRepository;
import com.side.hhplusecommerce.cart.service.CartItemService;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.service.CouponService;
import com.side.hhplusecommerce.coupon.service.dto.CouponUseResult;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemValidator;
import com.side.hhplusecommerce.item.service.ItemStockService;
import com.side.hhplusecommerce.order.controller.dto.CreateOrderResponse;
import com.side.hhplusecommerce.order.domain.Order;
import com.side.hhplusecommerce.order.domain.OrderItem;
import com.side.hhplusecommerce.order.service.ExternalDataPlatformService;
import com.side.hhplusecommerce.order.service.OrderPaymentService;
import com.side.hhplusecommerce.order.service.OrderService;
import com.side.hhplusecommerce.order.service.dto.OrderCreateResult;
import com.side.hhplusecommerce.payment.service.UserPointService;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderCreateUseCase {
    private final CartItemValidator cartItemValidator;
    private final CartItemService cartItemService;
    private final ItemValidator itemValidator;
    private final ItemStockService itemStockService;
    private final CouponService couponService;
    private final OrderService orderService;
    private final OrderPaymentService orderPaymentService;
    private final CartRepository cartRepository;
    private final ExternalDataPlatformService externalDataPlatformService;
    private final UserPointService userPointService;

    @Transactional
    public CreateOrderResponse create(Long userId, List<Long> cartItemIds, Long userCouponId) {

        // 장바구니 검증
        List<CartItem> validCartItems = cartItemValidator.validateOwnership(userId, cartItemIds);

        // 아이템 검증
        List<Item> items = itemValidator.validateExistence(getItemIds(validCartItems));

        // 쿠폰 검증 및 사용
        Coupon coupon = null;
        Integer couponDiscount = 0;
        if (Objects.nonNull(userCouponId)) {
            CouponUseResult couponUseResult = couponService.useCoupon(userCouponId);
            coupon = couponUseResult.getCoupon();
            couponDiscount = couponUseResult.getDiscountAmount();
        }

        Integer totalAmount = cartItemService.calculateTotalAmount(validCartItems, items);

        // 1. 주문 생성 (부모 트랜잭션)
        OrderCreateResult orderCreateResult = orderService.createOrder(
                userId, validCartItems, items, totalAmount, couponDiscount, userCouponId);

        Order savedOrder = orderCreateResult.getOrder();
        List<OrderItem> savedOrderItems = orderCreateResult.getOrderItems();

        // 2. 재고 확인 및 차감 (자식 트랜잭션 REQUIRES_NEW + 비관적 락)
        try {
            itemStockService.decreaseStock(validCartItems, items);
        } catch (Exception e) {
            // 재고 차감 실패 시: 주문(1)과 재고(2) 모두 롤백
            throw e;
        }

        // 3. 포인트 사용 (자식 트랜잭션 REQUIRES_NEW + 비관적 락)
        try {
            userPointService.use(userId, savedOrder.getFinalAmount());
        } catch (Exception e) {
            // 포인트 사용 실패 시: 주문(1)과 포인트(3)만 롤백, 재고(2)는 이미 커밋됨
            // 보상 트랜잭션으로 재고 복구
            itemStockService.increaseStock(validCartItems, items);
            throw e;
        }

        // 4. 주문 상태를 결제 완료로 변경 (부모 트랜잭션)
        orderService.completeOrderPayment(savedOrder);

        // 비동기로 장바구니 삭제 (주문 완료 후)
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (Objects.nonNull(cart)) {
            cartItemService.deleteCartItemsAsync(cart.getCartId());
        }

        // 비동기로 외부 데이터 플랫폼 전송
        externalDataPlatformService.sendOrderDataAsync(savedOrder);

        return CreateOrderResponse.of(savedOrder, savedOrderItems, coupon);
    }

    private List<Long> getItemIds(List<CartItem> validCartItems) {
        return validCartItems.stream()
                .map(CartItem::getItemId)
                .toList();
    }
}
