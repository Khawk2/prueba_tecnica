package com.nequi.franchises.infrastructure.config;

import com.nequi.franchises.infrastructure.config.properties.ApplicationProperties;
import com.nequi.franchises.infrastructure.service.DistributedRateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Rate Limiting Filter - Distributed implementation using Redis.
 * 
 * ORDER: -90 (after CorrelationIdFilter [-100], before AuthFilter)
 * 
 * TIERS:
 * 1. Global: 10K req/min total
 * 2. Per-IP: 100 req/min (standard), 5 req/min (auth)
 * 3. Per-User: 1000 req/min (authenticated)
 * 
 * NOTE: Only active when DistributedRateLimitingService is available (Redis enabled)
 */
@Slf4j
@Component
@Order(-90)
@RequiredArgsConstructor
@ConditionalOnBean(DistributedRateLimitingService.class)
public class RateLimitingFilter implements WebFilter {

    private final DistributedRateLimitingService rateLimitingService;
    private final ApplicationProperties appProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Skip rate limiting if feature is disabled
        if (appProperties.features() != null && 
            appProperties.features().rateLimiting() != null && 
            !appProperties.features().rateLimiting()) {
            log.debug("Rate limiting disabled via feature flag");
            return chain.filter(exchange);
        }
        
        String path = exchange.getRequest().getPath().value();
        String clientIp = getClientIp(exchange);
        
        // Check if this is an auth endpoint
        boolean isAuthEndpoint = path.contains("/auth/");
        
        // Select appropriate limit
        Mono<DistributedRateLimitingService.RateLimitResult> check = isAuthEndpoint
                ? rateLimitingService.checkAuthLimit(clientIp)
                : rateLimitingService.checkStandardLimit(clientIp);
        
        return check.flatMap(result -> {
            if (result.allowed()) {
                // Add rate limit headers
                exchange.getResponse().getHeaders().add("X-RateLimit-Limit", 
                        String.valueOf(result.windowSeconds() == 60 ? 100 : 5));
                exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", 
                        String.valueOf(result.remainingRequests()));
                
                return chain.filter(exchange);
            } else {
                // Rate limit exceeded
                log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("Retry-After", "60");
                return exchange.getResponse().setComplete();
            }
        });
    }
    
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }
}
