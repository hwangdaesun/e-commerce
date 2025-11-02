package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.IssueCouponRequest;
import com.side.hhplusecommerce.user.controller.dto.IssueCouponResponse;
import com.side.hhplusecommerce.user.controller.dto.UserCouponsResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class CouponController implements CouponControllerDocs {

    @Override
    @GetMapping("/api/users/{userId}/coupons")
    public ResponseEntity<UserCouponsResponse> getUserCoupons(@PathVariable Long userId) {
        // Mock 데이터
        List<UserCouponsResponse.UserCoupon> coupons = List.of(
                new UserCouponsResponse.UserCoupon(
                        1L,
                        1L,
                        "신규 가입 쿠폰",
                        5000,
                        false,
                        null,
                        LocalDateTime.now().plusMonths(2),
                        LocalDateTime.now()
                ),
                new UserCouponsResponse.UserCoupon(
                        2L,
                        2L,
                        "3월 특별 할인 쿠폰",
                        10000,
                        true,
                        LocalDateTime.now().minusDays(5),
                        LocalDateTime.now().plusMonths(1),
                        LocalDateTime.now().minusDays(10)
                )
        );

        UserCouponsResponse response = new UserCouponsResponse(coupons);
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/api/coupons/{couponId}/issue")
    public ResponseEntity<IssueCouponResponse> issueCoupon(
            @PathVariable Long couponId,
            @RequestBody IssueCouponRequest request
    ) {
        // Mock 데이터
        IssueCouponResponse response = new IssueCouponResponse(
                1L,
                1L,
                "신규 가입 쿠폰",
                5000,
                false,
                LocalDateTime.now().plusMonths(2),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
