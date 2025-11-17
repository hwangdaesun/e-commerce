package com.side.hhplusecommerce.coupon.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.side.hhplusecommerce.ContainerTest;
import com.side.hhplusecommerce.coupon.controller.dto.IssueCouponRequest;
import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.CouponStock;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.CouponStockRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CouponIntegrationTest extends ContainerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponStockRepository couponStockRepository;

    @Autowired
    private UserCouponRepository userCouponRepository;

    private Long userWithCouponId;     // 쿠폰을 보유한 사용자 ID
    private Long userWithoutCouponId;  // 쿠폰이 없는 사용자 ID
    private Long issuedCouponId;       // 이미 발급받은 쿠폰 ID
    private Long availableCouponId;    // 발급 가능한 쿠폰 ID

    @BeforeEach
    void setUp() {
        userWithCouponId = 100L;
        userWithoutCouponId = 101L;

        // 테스트 데이터 준비
        // 첫 번째 쿠폰 (이미 발급받은 쿠폰)
        Coupon coupon1 = Coupon.builder()
                .name("3000원 할인 쿠폰")
                .discountAmount(3000)
                .totalQuantity(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        Coupon savedCoupon1 = couponRepository.save(coupon1);
        issuedCouponId = savedCoupon1.getCouponId();

        CouponStock stock1 = CouponStock.of(issuedCouponId, 10);
        couponStockRepository.save(stock1);

        // 이미 발급받은 쿠폰
        UserCoupon userCoupon = UserCoupon.issue(userWithCouponId, issuedCouponId);
        userCouponRepository.save(userCoupon);

        // 두 번째 쿠폰 (발급 테스트용)
        Coupon coupon2 = Coupon.builder()
                .name("5000원 할인 쿠폰")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        Coupon savedCoupon2 = couponRepository.save(coupon2);
        availableCouponId = savedCoupon2.getCouponId();

        CouponStock stock2 = CouponStock.of(availableCouponId, 10);
        couponStockRepository.save(stock2);
    }

    @Test
    @DisplayName("[성공] 사용자 쿠폰 목록 조회")
    void getUserCoupons_success() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/coupons", userWithCouponId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupons[0].couponId").value(issuedCouponId));
    }

    @Test
    @DisplayName("[성공] 사용자 쿠폰 목록 조회 - 쿠폰이 없는 경우")
    void getUserCoupons_success_empty() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/coupons", userWithoutCouponId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupons").isArray())
                .andExpect(jsonPath("$.coupons").isEmpty());
    }

    @Test
    @DisplayName("[성공] 쿠폰 발급")
    void issueCoupon_success() throws Exception {
        IssueCouponRequest request = new IssueCouponRequest(userWithCouponId);

        mockMvc.perform(post("/api/coupons/{couponId}/issue", availableCouponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
