package com.side.hhplusecommerce.order.service;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.coupon.service.CouponService;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.service.ItemStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderRollbackHandler {
    private final CouponService couponService;
    private final ItemStockService itemStockService;
    private final OrderService orderService;
    /**
     * 재고 차감 실패 시: 쿠폰만 롤백
     */
    public void rollbackForStockFailure(Long orderId, Long userCouponId) {
        log.info("Starting rollback for stock failure: userCouponId={}", userCouponId);
        orderService.failOrder(orderId);
        rollbackCoupon(userCouponId);
    }

    /**
     * 주문 생성 실패 시: 쿠폰 + 재고 롤백
     */
    public void rollbackForOrderCreationFailure(Long userCouponId, List<CartItem> cartItems, List<Item> items) {
        log.info("Starting rollback for order creation failure: userCouponId={}", userCouponId);
        rollbackStock(cartItems, items);
        rollbackCoupon(userCouponId);
    }

    /**
     * 결제 처리 실패 시: 쿠폰 + 재고 롤백 (주문은 취소 상태로 유지)
     */
    public void rollbackForPaymentFailure(Long orderId, Long userCouponId, List<CartItem> cartItems, List<Item> items) {
        log.info("Starting rollback for payment failure: userCouponId={}", userCouponId);
        orderService.failOrder(orderId);
        rollbackStock(cartItems, items);
        rollbackCoupon(userCouponId);
    }

    private void rollbackCoupon(Long userCouponId) {
        if (Objects.isNull(userCouponId)) {
            return;
        }

        try {
            couponService.cancelCouponUse(userCouponId);
            log.info("Successfully rolled back coupon use: userCouponId={}", userCouponId);
        } catch (Exception e) {
            log.error("Failed to rollback coupon use: userCouponId={}", userCouponId, e);
        }
    }

    private void rollbackStock(List<CartItem> cartItems, List<Item> items) {
        try {
            itemStockService.increaseStock(cartItems, items);
            log.info("Successfully rolled back stock for {} items", cartItems.size());
        } catch (Exception e) {
            log.error("Failed to rollback stock", e);
        }
    }
}
