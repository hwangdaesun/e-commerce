package com.side.hhplusecommerce.common.lock.distributed;

import com.side.hhplusecommerce.common.lock.LockKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * 분산 락 AOP
 * @DistributedLock 어노테이션이 붙은 메서드에 대해 Redisson을 사용한 분산 락을 적용합니다.
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final RedissonClient redissonClient;
    private final ApplicationContext applicationContext;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        // LockKeyResolver 빈을 가져와서 프리픽스 획득
        LockKeyResolver keyResolver = applicationContext.getBean(
                distributedLock.keyResolver(),
                LockKeyResolver.class
        );
        String keyPrefix = keyResolver.getKeyPrefix();

        // SpEL을 사용하여 키 값 추출
        String keyValue = parseSpELExpression(joinPoint, distributedLock.key());

        // 최종 락 키 생성: prefix:value
        String lockKey = keyPrefix + ":" + keyValue;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(
                    distributedLock.waitTime(),
                    distributedLock.leaseTime(),
                    distributedLock.timeUnit()
            );

            if (!acquired) {
                log.warn("Failed to acquire lock: {}", lockKey);
                throw new IllegalStateException("락 획득에 실패했습니다. 잠시 후 다시 시도해주세요.");
            }

            log.debug("Lock acquired: {}", lockKey);
            return joinPoint.proceed();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while acquiring lock: {}", lockKey, e);
            throw new IllegalStateException("락 획득 중 인터럽트가 발생했습니다.", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", lockKey);
            }
        }
    }

    /**
     * SpEL 표현식을 파싱하여 실제 값을 추출합니다.
     *
     * @param joinPoint 메서드 실행 정보
     * @param expression SpEL 표현식 (예: "#userId", "#request.userId")
     * @return 파싱된 값의 문자열 표현
     */
    private String parseSpELExpression(ProceedingJoinPoint joinPoint, String expression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        StandardEvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }

        Object value = parser.parseExpression(expression).getValue(context);
        if (value == null) {
            throw new IllegalArgumentException("락 키 값이 null입니다: " + expression);
        }

        return value.toString();
    }
}
