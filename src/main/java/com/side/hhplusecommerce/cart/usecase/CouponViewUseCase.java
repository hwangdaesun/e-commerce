package com.side.hhplusecommerce.cart.usecase;

import com.side.hhplusecommerce.coupon.domain.Coupon;
import com.side.hhplusecommerce.coupon.domain.UserCoupon;
import com.side.hhplusecommerce.coupon.repository.CouponRepository;
import com.side.hhplusecommerce.coupon.repository.UserCouponRepository;
import com.side.hhplusecommerce.coupon.controller.dto.UserCouponsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponViewUseCase {
    private final UserCouponRepository userCouponRepository;
    private final CouponRepository couponRepository;

    public UserCouponsResponse viewUserCoupons(Long userId) {
        List<UserCoupon> userCoupons = userCouponRepository.findByUserId(userId);

        if (userCoupons.isEmpty()) {
            return new UserCouponsResponse(Collections.emptyList());
        }

        List<Long> couponIds = userCoupons.stream()
                .map(UserCoupon::getCouponId)
                .toList();

        Map<Long, Coupon> couponMap = couponIds.stream()
                .map(couponRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Coupon::getCouponId, coupon -> coupon));

        return UserCouponsResponse.of(userCoupons, couponMap);
    }
}
