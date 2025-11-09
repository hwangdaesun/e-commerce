package com.side.hhplusecommerce.common.lock.aspect;

import com.side.hhplusecommerce.common.lock.OptimisticLock;
import com.side.hhplusecommerce.common.lock.exception.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 낙관적 락을 처리하는 Aspect
 * Map<Long, AtomicLong>을 사용하여 버전 관리
 */
@Aspect
@Component
@Slf4j
public class OptimisticLockAspect {

    private final Map<Long, AtomicLong> versionMap = new ConcurrentHashMap<>();

    @Around("@annotation(optimisticLock) && args(resourceId, ..)")
    public Object handleOptimisticLock(ProceedingJoinPoint joinPoint, OptimisticLock optimisticLock, long resourceId) throws Throwable {
        int maxRetries = optimisticLock.maxRetries();
        long retryDelay = optimisticLock.retryDelay();

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                // 현재 버전 가져오기
                AtomicLong version = versionMap.computeIfAbsent(resourceId, key -> new AtomicLong(0));
                long currentVersion = version.get();

                log.debug("Optimistic lock attempt {}/{} for resource {} with version {}",
                    attempt + 1, maxRetries, resourceId, currentVersion);

                // 비즈니스 로직 실행
                Object result = joinPoint.proceed();

                // 버전 증가 (CAS 연산)
                if (version.compareAndSet(currentVersion, currentVersion + 1)) {
                    log.debug("Optimistic lock succeeded for resource {} (version {} -> {})",
                        resourceId, currentVersion, currentVersion + 1);
                    return result;
                } else {
                    // 버전이 변경되었으면 충돌 발생
                    log.debug("Optimistic lock conflict detected for resource {} at attempt {}",
                        resourceId, attempt + 1);

                    if (attempt < maxRetries - 1) {
                        // 재시도 전 대기
                        optimisticLock.timeUnit().sleep(retryDelay);
                    }
                }
            } catch (Exception e) {
                log.error("Error during optimistic lock execution for resource {}: {}",
                    resourceId, e.getMessage());
                throw e;
            }
        }

        // 최대 재시도 횟수 초과
        log.error("Optimistic lock failed after {} attempts for resource {}", maxRetries, resourceId);
        throw new OptimisticLockException("최대 재시도 횟수를 초과했습니다. resourceId: " + resourceId);
    }
    
}