package com.side.hhplusecommerce.coupon.domain;

import com.side.hhplusecommerce.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    private Coupon(Long couponId, String name, Integer discountAmount, Integer totalQuantity, LocalDateTime expiresAt) {
        super();
        this.couponId = couponId;
        this.name = name;
        this.discountAmount = discountAmount;
        this.totalQuantity = totalQuantity;
        this.expiresAt = expiresAt;
    }
}