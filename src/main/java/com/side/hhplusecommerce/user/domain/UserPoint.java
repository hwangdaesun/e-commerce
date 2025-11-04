package com.side.hhplusecommerce.user.domain;

import com.side.hhplusecommerce.user.exception.InsufficientPointException;
import com.side.hhplusecommerce.user.exception.InvalidPointAmountException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserPoint {
    private Long userId;
    private Integer point;
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
