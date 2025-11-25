package com.side.hhplusecommerce.order.service;

import com.side.hhplusecommerce.cart.domain.CartItem;
import com.side.hhplusecommerce.common.exception.CustomException;
import com.side.hhplusecommerce.common.exception.ErrorCode;
import com.side.hhplusecommerce.item.domain.Item;
import com.side.hhplusecommerce.order.service.dto.OrderCreateResult;
import com.side.hhplusecommerce.item.service.ItemStockService;
import com.side.hhplusecommerce.payment.service.UserPointService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderTransactionService {
    private final OrderService orderService;
    private final ItemStockService itemStockService;
    private final UserPointService userPointService;
    private final OrderRollbackHandler orderRollbackHandler;

    @Transactional
    public OrderCreateResult executeCoreOrderTransaction(
            Long userId,
            List<CartItem> validCartItems,
            List<Item> items,
            Integer totalAmount,
            Integer couponDiscount,
            Long userCouponId) {

        // 1. 주문 생성 (부모 트랜잭션)
        OrderCreateResult orderCreateResult = orderService.createOrder(
                userId, validCartItems, items, totalAmount, couponDiscount, userCouponId);

        // 2. 재고 확인 및 차감 (자식 트랜잭션 REQUIRES_NEW + 비관적 락)
        try {
            for (CartItem cartItem : validCartItems) {
                itemStockService.decreaseStockForItem(cartItem.getItemId(), cartItem.getQuantity());
            }
        } catch (Exception e) {
            // 보상 트랜잭션으로 주문 취소, 쿠폰 사용 해제
            orderRollbackHandler.rollbackForStockFailure(userCouponId, orderCreateResult.getOrderId());
            throw new CustomException(ErrorCode.FAIL_ORDER);
        }

        // 3. 포인트 사용 (자식 트랜잭션 REQUIRES_NEW + 비관적 락)
        try {
            userPointService.use(userId, orderCreateResult.getFinalAmount());
        } catch (Exception e) {
            // 보상 트랜잭션으로 주문 취소, 재고 복구,  쿠폰 사용 해제
            orderRollbackHandler.rollbackForPaymentFailure(orderCreateResult.getOrderId(), userCouponId, validCartItems, items);
            throw new CustomException(ErrorCode.FAIL_ORDER);
        }

        // 4. 주문 상태를 결제 완료로 변경 (부모 트랜잭션)
        orderService.completeOrderPayment(orderCreateResult.getOrderId());

        return orderCreateResult;
    }
}
