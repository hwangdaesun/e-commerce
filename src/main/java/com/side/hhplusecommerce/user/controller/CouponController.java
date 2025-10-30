package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.UserCouponsResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

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

}
