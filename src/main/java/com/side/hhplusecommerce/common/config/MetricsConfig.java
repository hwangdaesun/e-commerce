package com.side.hhplusecommerce.common.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Micrometer 메트릭 설정
 *
 * - @Timed 애노테이션을 사용한 메서드 실행 시간 측정 활성화
 * - 커스텀 메트릭 등록을 위한 설정
 */
@Configuration
public class MetricsConfig {

    /**
     * @Timed 애노테이션 활성화
     *
     * 이 빈을 등록하면 @Timed 애노테이션이 붙은 메서드의 실행 시간이 자동으로 측정됩니다.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}