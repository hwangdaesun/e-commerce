package com.side.hhplusecommerce.coupon.controller;

import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponRequest;
import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponResponse;
import com.side.hhplusecommerce.coupon.controller.dto.UserCouponsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "쿠폰", description = "쿠폰 API")
public interface CouponControllerDocs {

    @Operation(summary = "사용자 쿠폰 조회", description = "현재 사용자가 보유한 쿠폰 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<UserCouponsResponse> getUserCoupons(
            @Parameter(description = "사용자 ID", required = true)
            Long userId
    );

    @Operation(summary = "쿠폰 발급", description = "특정 쿠폰을 사용자에게 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "발급 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 쿠폰"),
            @ApiResponse(responseCode = "409", description = "쿠폰 재고 소진 또는 이미 발급받은 쿠폰"),
            @ApiResponse(responseCode = "410", description = "만료된 쿠폰"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    ResponseEntity<IssueCouponResponse> issueCoupon(
            @Parameter(description = "발급받을 쿠폰 ID", required = true)
            Long couponId,
            @RequestBody IssueCouponRequest request
    );
}
