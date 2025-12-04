package com.side.hhplusecommerce.coupon.controller;

import com.side.hhplusecommerce.cart.usecase.CouponIssueUseCase;
import com.side.hhplusecommerce.cart.usecase.CouponViewUseCase;
import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponRequest;
import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponResponse;
import com.side.hhplusecommerce.coupon.controller.dto.UserCouponsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponController implements CouponControllerDocs {
    private final CouponViewUseCase couponViewUseCase;
    private final CouponIssueUseCase couponIssueUseCase;

    @Override
    @GetMapping("/api/users/{userId}/coupons")
    public ResponseEntity<UserCouponsResponse> getUserCoupons(@PathVariable Long userId) {
        UserCouponsResponse response = couponViewUseCase.viewUserCoupons(userId);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/api/coupons/{couponId}/issue")
    public ResponseEntity<IssueCouponResponse> issueCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody IssueCouponRequest request
    ) {
        // 비동기 큐 방식으로 변경되어 즉시 응답 반환
        couponIssueUseCase.issue(couponId, request.getUserId());
        // 202 Accepted: 요청이 접수되었으나 처리가 완료되지 않음
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
