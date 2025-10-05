package com.basic.project.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class RateLimitConfig {

    @Value("${rate-limit.login.requests:5}")
    private int loginRequests;

    @Value("${rate-limit.login.period:1}")
    private int loginPeriodMinutes;

    @Value("${rate-limit.register.requests:3}")
    private int registerRequests;

    @Value("${rate-limit.register.period:60}")
    private int registerPeriodMinutes;

    @Value("${rate-limit.refresh.requests:10}")
    private int refreshRequests;

    @Value("${rate-limit.refresh.period:1}")
    private int refreshPeriodMinutes;

    @Bean
    public Bucket loginRateLimitBucket() {
        // Настраиваемые лимиты для входа
        Bandwidth limit = Bandwidth.classic(loginRequests, 
                Refill.greedy(loginRequests, Duration.ofMinutes(loginPeriodMinutes)));
        log.info("Login rate limit configured: {} requests per {} minutes", 
                loginRequests, loginPeriodMinutes);
        return Bucket.builder().addLimit(limit).build();
    }

    @Bean
    public Bucket registerRateLimitBucket() {
        // Настраиваемые лимиты для регистрации
        Bandwidth limit = Bandwidth.classic(registerRequests, 
                Refill.greedy(registerRequests, Duration.ofMinutes(registerPeriodMinutes)));
        log.info("Register rate limit configured: {} requests per {} minutes", 
                registerRequests, registerPeriodMinutes);
        return Bucket.builder().addLimit(limit).build();
    }

    @Bean
    public Bucket refreshRateLimitBucket() {
        // Настраиваемые лимиты для обновления токенов
        Bandwidth limit = Bandwidth.classic(refreshRequests, 
                Refill.greedy(refreshRequests, Duration.ofMinutes(refreshPeriodMinutes)));
        log.info("Refresh rate limit configured: {} requests per {} minutes", 
                refreshRequests, refreshPeriodMinutes);
        return Bucket.builder().addLimit(limit).build();
    }
}
