package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.IssueCouponRequest;
import com.side.hhplusecommerce.user.controller.dto.IssueCouponResponse;
import com.side.hhplusecommerce.user.controller.dto.UserCouponsResponse;
import com.side.hhplusecommerce.user.usecase.CouponIssueUseCase;
import com.side.hhplusecommerce.user.usecase.CouponViewUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
            @RequestBody IssueCouponRequest request
    ) {
        IssueCouponResponse response = couponIssueUseCase.issue(couponId, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
