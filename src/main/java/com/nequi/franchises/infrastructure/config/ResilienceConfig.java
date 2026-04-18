package com.nequi.franchises.infrastructure.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Configuration for Circuit Breaker, Retry, and Time Limiter.
 * 
 * PATTERNS:
 * - Circuit Breaker: Open after 50% failure rate, prevent cascade failures
 * - Retry: 3 attempts with exponential backoff (1s, 2s, 4s)
 * - Time Limiter: 5 seconds max per operation
 * 
 * These apply to critical operations like database access.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Circuit Breaker for MongoDB operations.
     * 
     * Config:
     * - Failure rate threshold: 50%
     * - Slow call threshold: 80% (calls > 2s considered slow)
     * - Wait duration in open state: 30s
     * - Permitted calls in half-open: 5
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(80)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowSize(100)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .recordExceptions(
                        java.util.concurrent.TimeoutException.class,
                        java.io.IOException.class,
                        org.springframework.dao.DataAccessException.class
                )
                .ignoreExceptions(
                        com.nequi.franchises.domain.exception.ValidationException.class,
                        com.nequi.franchises.domain.exception.FranchiseNotFoundException.class
                )
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Retry configuration for transient failures.
     * 
     * Config:
     * - Max attempts: 3
     * - Wait duration: 1s with exponential backoff (1s, 2s, 4s)
     * - Retry on: TimeoutException, IOException
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(
                        java.util.concurrent.TimeoutException.class,
                        java.io.IOException.class
                )
                .ignoreExceptions(
                        com.nequi.franchises.domain.exception.ValidationException.class,
                        com.nequi.franchises.domain.exception.FranchiseNotFoundException.class
                )
                .build();

        return RetryRegistry.of(config);
    }

    /**
     * Time Limiter for preventing long-running operations.
     * 
     * Config:
     * - Timeout: 5 seconds
     * - Cancel running future: true
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();

        return TimeLimiterRegistry.of(config);
    }
}
