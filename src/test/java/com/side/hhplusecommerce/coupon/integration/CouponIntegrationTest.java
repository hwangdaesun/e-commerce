package com.side.hhplusecommerce.coupon.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class CouponIntegrationTest {

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

    @BeforeEach
    void setUp() {
        // 모든 데이터 정리 (테스트 격리)
        userCouponRepository.deleteAll();
        couponStockRepository.deleteAll();
        couponRepository.deleteAll();

        // 테스트 데이터 준비
        Coupon coupon1 = Coupon.builder()
                .couponId(1L)
                .name("5000원 할인 쿠폰")
                .discountAmount(5000)
                .totalQuantity(100)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        couponRepository.save(coupon1);

        CouponStock stock1 = CouponStock.of(1L, 10);
        couponStockRepository.save(stock1);

        // 이미 발급받은 쿠폰 (userId=100, couponId=1)
        UserCoupon userCoupon = UserCoupon.issue(100L, 1L);
        userCouponRepository.save(userCoupon);
    }

    @Test
    @DisplayName("[성공] 사용자 쿠폰 목록 조회")
    void getUserCoupons_success() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/coupons", 100L))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("[성공] 사용자 쿠폰 목록 조회 - 쿠폰이 없는 경우")
    void getUserCoupons_success_empty() throws Exception {
        mockMvc.perform(get("/api/users/{userId}/coupons", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coupons").isArray())
                .andExpect(jsonPath("$.coupons").isEmpty());
    }

    @Test
    @DisplayName("[성공] 쿠폰 발급")
    void issueCoupon_success() throws Exception {
        IssueCouponRequest request = new IssueCouponRequest(200L);

        mockMvc.perform(post("/api/coupons/{couponId}/issue", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
