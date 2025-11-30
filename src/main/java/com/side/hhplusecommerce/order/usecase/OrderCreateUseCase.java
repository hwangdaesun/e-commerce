package com.side.hhplusecommerce.order.usecase;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.cart.domain.CartItemValidator;
import com.side.hhplusecommerce.cart.service.CartItemService;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.service.CouponService;
import com.side.hhplusecommerce.coupon.service.dto.CouponUseResult;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.item.domain.ItemValidator;
import com.side.hhplusecommerce.order.controller.dto.CreateOrderResponse;
import com.side.hhplusecommerce.order.service.ExternalDataPlatformService;
import com.side.hhplusecommerce.order.service.OrderTransactionService;
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
    private final CouponService couponService;
    private final ExternalDataPlatformService externalDataPlatformService;
    private final OrderTransactionService orderTransactionService;

    public CreateOrderResponse create(Long userId, List<Long> cartItemIds, Long userCouponId) {

        // 1. 검증 단계 (트랜잭션 없음 - 읽기만 수행)
        List<CartItem> validCartItems = cartItemValidator.validateOwnership(userId, cartItemIds);
        List<Item> items = itemValidator.validateExistence(getItemIds(validCartItems));
        Integer totalAmount = cartItemService.calculateTotalAmount(validCartItems, items);

        // 2. 쿠폰 검증 및 사용
        Coupon coupon = null;
        Integer couponDiscount = 0;
        if (Objects.nonNull(userCouponId)) {
            CouponUseResult couponUseResult = couponService.useCoupon(userCouponId);
            coupon = couponUseResult.getCoupon();
            couponDiscount = couponUseResult.getDiscountAmount();
        }

        // 3. 핵심 주문 처리 (트랜잭션 내부)
        OrderCreateResult orderCreateResult = orderTransactionService.executeCoreOrderTransaction(
                userId, validCartItems, items, totalAmount, couponDiscount, userCouponId);

        // 4. 비동기 후처리 (트랜잭션 밖)
        cartItemService.deleteCartItemsAsync(validCartItems.get(0).getCartId());

        externalDataPlatformService.sendOrderDataAsync(orderCreateResult.getOrderId());

        return CreateOrderResponse.of(orderCreateResult, coupon);
    }

    private List<Long> getItemIds(List<CartItem> validCartItems) {
        return validCartItems.stream()
                .map(CartItem::getItemId)
                .toList();
    }
}
