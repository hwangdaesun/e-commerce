package com.side.hhplusecommerce.user.controller;

import com.side.hhplusecommerce.user.controller.dto.UserCouponsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

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

}
