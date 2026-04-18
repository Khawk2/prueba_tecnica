package com.nequi.franchises.infrastructure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * Distributed Rate Limiting using Redis with Lua scripts.
 * 
 * ADVANTAGES:
 * - Works across multiple application instances
 * - Atomic operations via Lua scripts (no race conditions)
 * - Sliding window algorithm
 * - Low overhead (single Redis round-trip)
 * 
 * LIMITS:
 * - Standard API: 100 requests per minute per IP/user
 * - Auth API: 5 requests per minute per IP
 * 
 * NOTE: Disabled when app.features.rate-limiting=false or redis=false
 */
// ROOT CAUSE FIX: Servicio seguro con @ConditionalOnProperty.
// Solo se crea si rate-limiting está explícitamente habilitado.
// Esto evita errores de startup si Redis no está disponible.
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "app.features.rate-limiting", 
    havingValue = "true", 
    matchIfMissing = false  // Requerir configuración explícita
)
public class DistributedRateLimitingService {

    private final ReactiveStringRedisTemplate redisTemplate;

    // Lua script for atomic rate limiting with sliding window
    private static final String RATE_LIMIT_SCRIPT = 
        "local key = KEYS[1]\n" +
        "local window = tonumber(ARGV[1])\n" +
        "local limit = tonumber(ARGV[2])\n" +
        "local now = tonumber(ARGV[3])\n" +
        "local clearBefore = now - window\n" +
        "\n" +
        "-- Remove old entries outside window\n" +
        "redis.call('ZREMRANGEBYSCORE', key, 0, clearBefore)\n" +
        "\n" +
        "-- Count current requests in window\n" +
        "local current = redis.call('ZCARD', key)\n" +
        "\n" +
        "if current < limit then\n" +
        "    -- Add current request\n" +
        "    redis.call('ZADD', key, now, now .. ':' .. math.random(1000000))\n" +
        "    redis.call('EXPIRE', key, window)\n" +
        "    return {1, limit - current - 1}\n" +
        "else\n" +
        "    return {0, 0}\n" +
        "end";

    @SuppressWarnings("unchecked")
    private final RedisScript<List<Long>> rateLimitScript = RedisScript.of(RATE_LIMIT_SCRIPT, (Class<List<Long>>) (Class<?>) List.class);

    // Limits
    private static final long STANDARD_WINDOW_SECONDS = 60;
    private static final long STANDARD_LIMIT = 100;
    private static final long AUTH_WINDOW_SECONDS = 60;
    private static final long AUTH_LIMIT = 5;

    /**
     * Check rate limit for standard API endpoints.
     * Key format: ratelimit:standard:{identifier}
     */
    public Mono<RateLimitResult> checkStandardLimit(String identifier) {
        return checkLimit("standard", identifier, STANDARD_WINDOW_SECONDS, STANDARD_LIMIT);
    }

    /**
     * Check rate limit for auth endpoints (stricter).
     * Key format: ratelimit:auth:{ip}
     */
    public Mono<RateLimitResult> checkAuthLimit(String ipAddress) {
        return checkLimit("auth", ipAddress, AUTH_WINDOW_SECONDS, AUTH_LIMIT);
    }

    /**
     * Check rate limit for specific user (authenticated).
     * Key format: ratelimit:user:{userId}
     */
    public Mono<RateLimitResult> checkUserLimit(String userId) {
        return checkLimit("user", userId, STANDARD_WINDOW_SECONDS, STANDARD_LIMIT);
    }

    private Mono<RateLimitResult> checkLimit(String type, String identifier, long windowSeconds, long limit) {
        String key = String.format("ratelimit:%s:%s", type, identifier);
        long now = System.currentTimeMillis() / 1000;

        return redisTemplate.execute(rateLimitScript, 
                List.of(key), 
                List.of(String.valueOf(windowSeconds), String.valueOf(limit), String.valueOf(now)))
                .next()
                .map(result -> {
                    boolean allowed = result.get(0) == 1;
                    long remaining = result.get(1);
                    return new RateLimitResult(allowed, remaining, windowSeconds);
                })
                .doOnSuccess(result -> {
                    if (!result.allowed()) {
                        log.warn("Rate limit exceeded for {}:{}", type, identifier);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Redis rate limit error for {}:{}, allowing request", type, identifier, e);
                    // Fail open - allow request if Redis is down
                    return Mono.just(new RateLimitResult(true, 0, windowSeconds));
                });
    }

    /**
     * Reset rate limit for an identifier (e.g., after manual review).
     */
    public Mono<Void> resetLimit(String type, String identifier) {
        String key = String.format("ratelimit:%s:%s", type, identifier);
        return redisTemplate.delete(key).then();
    }

    public record RateLimitResult(boolean allowed, long remainingRequests, long windowSeconds) {}
}
