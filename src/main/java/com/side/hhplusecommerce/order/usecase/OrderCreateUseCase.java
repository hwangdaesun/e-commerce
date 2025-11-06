package com.side.hhplusecommerce.order.usecase;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.domain.CartItemValidator;
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
import com.side.hhplusecommerce.order.service.OrderPaymentService;
import com.side.hhplusecommerce.order.service.OrderRollbackHandler;
import com.side.hhplusecommerce.order.service.OrderService;
import com.side.hhplusecommerce.order.service.dto.OrderCreateResult;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
    private final OrderRollbackHandler orderRollbackHandler;

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

        // 재고 확인 및 차감
        try {
            itemStockService.decreaseStock(validCartItems, items);
        } catch (Exception e) {
            // 재고 차감 실패 시: 쿠폰만 롤백
            orderRollbackHandler.rollbackForStockFailure(userCouponId);
            throw e;
        }

        Integer totalAmount = cartItemService.calculateTotalAmount(validCartItems, items);

        // 주문 생성
        OrderCreateResult orderCreateResult;
        try {
            orderCreateResult = orderService.createOrder(
                    userId, validCartItems, items, totalAmount, couponDiscount, userCouponId);
        } catch (Exception e) {
            // 주문 생성 실패 시: 쿠폰 + 재고 롤백
            orderRollbackHandler.rollbackForOrderCreationFailure(userCouponId, validCartItems, items);
            throw e;
        }

        Order savedOrder = orderCreateResult.getOrder();
        List<OrderItem> savedOrderItems = orderCreateResult.getOrderItems();

        // 결제 처리
        try {
            orderPaymentService.processOrderPayment(userId, savedOrder);
        } catch (Exception e) {
            // 결제 실패 시: 쿠폰 + 재고 롤백 (주문은 취소 상태로 유지)
            orderRollbackHandler.rollbackForPaymentFailure(userCouponId, validCartItems, items);
            throw e;
        }

        return CreateOrderResponse.of(savedOrder, savedOrderItems, coupon);
    }

    private List<Long> getItemIds(List<CartItem> validCartItems) {
        return validCartItems.stream()
                .map(CartItem::getItemId)
                .toList();
    }
}
