package com.nequi.franchises.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.UUID;

/**
 * WebFilter that adds correlation ID to all requests.
 * Provides distributed tracing capability for observability.
 * 
 * The correlation ID is:
 * - Extracted from header "X-Correlation-Id" if present
 * - Generated as UUID if not present
 * - Added to MDC for logging
 * - Added to response headers
 * - Propagated through Reactor Context
 */
@Slf4j
@Component
@Order(-100) // High priority to run first
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        final String correlationId = getOrGenerateCorrelationId(exchange);

        // Add to response headers
        exchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);

        // Store in MDC for logging and propagate through chain
        return chain.filter(exchange)
                .contextWrite(Context.of(CORRELATION_ID_KEY, correlationId))
                .doOnSubscribe(subscription -> MDC.put(CORRELATION_ID_KEY, correlationId))
                .doOnTerminate(() -> MDC.remove(CORRELATION_ID_KEY));
    }

    private String getOrGenerateCorrelationId(ServerWebExchange exchange) {
        String existingId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        return (existingId != null && !existingId.isEmpty()) ? existingId : generateCorrelationId();
    }

    private String generateCorrelationId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
