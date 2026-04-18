package com.nequi.franchises.infrastructure.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Micrometer Metrics Configuration for Business Metrics.
 * 
 * METRICS EXPOSED:
 * - Custom counters: franchises.created, products.created, branches.created
 * - Timers: operation.latency by endpoint
 * - Error rates by exception type
 * 
 * All metrics available at /actuator/prometheus
 */
@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {

    /**
     * Enable @Timed annotation for method timing.
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Counter for franchises created.
     */
    @Bean
    public Counter franchiseCreatedCounter(MeterRegistry registry) {
        return Counter.builder("franchises.created")
                .description("Total number of franchises created")
                .register(registry);
    }

    /**
     * Counter for branches created.
     */
    @Bean
    public Counter branchCreatedCounter(MeterRegistry registry) {
        return Counter.builder("branches.created")
                .description("Total number of branches created")
                .register(registry);
    }

    /**
     * Counter for products created.
     */
    @Bean
    public Counter productCreatedCounter(MeterRegistry registry) {
        return Counter.builder("products.created")
                .description("Total number of products created")
                .register(registry);
    }

    /**
     * Counter for errors by type.
     */
    @Bean
    public Counter errorCounter(MeterRegistry registry) {
        return Counter.builder("api.errors")
                .description("Total number of API errors")
                .tag("type", "unknown") // Will be customized at runtime
                .register(registry);
    }

    /**
     * Timer for top product report generation.
     */
    @Bean
    public Timer topProductReportTimer(MeterRegistry registry) {
        return Timer.builder("reports.top_products.latency")
                .description("Time taken to generate top products report")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }
}
