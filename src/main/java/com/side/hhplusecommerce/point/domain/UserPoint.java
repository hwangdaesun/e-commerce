package com.side.hhplusecommerce.point.domain;

import com.side.hhplusecommerce.point.exception.InsufficientPointException;
import com.side.hhplusecommerce.point.exception.InvalidPointAmountException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPoint {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "point", nullable = false)
    private Integer point;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder(access = AccessLevel.PRIVATE)
    private UserPoint(Long userId, Integer point) {
        this.userId = userId;
        this.point = point;
        this.updatedAt = LocalDateTime.now();
    }

    public static UserPoint initialize(Long userId) {
        return UserPoint.builder()
                .userId(userId)
                .point(0)
                .build();
    }

    public void charge(Integer amount) {
        validateAmount(amount);
        this.point += amount;
        this.updatedAt = LocalDateTime.now();
    }

    public void use(Integer amount) {
        validateAmount(amount);
        validateSufficientPoint(amount);
        this.point -= amount;
        this.updatedAt = LocalDateTime.now();
    }

    private void validateAmount(Integer amount) {
        if (amount < 1) {
            throw new InvalidPointAmountException();
        }
    }

    private void validateSufficientPoint(Integer amount) {
        if (this.point < amount) {
            throw new InsufficientPointException();
        }
    }
}
