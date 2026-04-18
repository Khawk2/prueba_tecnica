package com.nequi.franchises.concurrency;

import com.nequi.franchises.infrastructure.service.IdempotencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests for idempotency service.
 * 
 * GOAL: Verify that idempotency handles race conditions correctly.
 * PATTERN: Multiple concurrent requests with same idempotency key.
 * EXPECTED: Only first request processes, others get ALREADY_PROCESSED.
 */
@SpringBootTest
@Testcontainers
@Tag("integration")
class IdempotencyConcurrencyTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers handles lifecycle automatically
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;

    @Test
    @DisplayName("Concurrent requests with same idempotency key - only one should succeed")
    void concurrentIdempotencyCheck() {
        // Given
        String idempotencyKey = "test-key-" + System.currentTimeMillis();
        int concurrentRequests = 10;

        // When - Simulate 10 concurrent requests with same key
        Flux<IdempotencyService.IdempotencyResult> results = Flux.range(0, concurrentRequests)
                .flatMap(i -> idempotencyService.acquireLock(idempotencyKey))
                .onErrorContinue((err, obj) -> {
                    // Ignore errors from duplicate key - that's expected
                });

        // Then - Collect results and verify
        AtomicInteger newCount = new AtomicInteger(0);
        AtomicInteger alreadyProcessedCount = new AtomicInteger(0);

        StepVerifier.create(results.collectList())
                .assertNext(list -> {
                    for (IdempotencyService.IdempotencyResult r : list) {
                        if (r.status() == IdempotencyService.Status.NEW) {
                            newCount.incrementAndGet();
                        } else if (r.status() == IdempotencyService.Status.ALREADY_PROCESSED) {
                            alreadyProcessedCount.incrementAndGet();
                        }
                    }
                    // Verify only one succeeded
                    assertThat(newCount.get()).isEqualTo(1);
                    assertThat(alreadyProcessedCount.get()).isEqualTo(concurrentRequests - 1);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Idempotency complete processing should store resource id")
    void completeProcessingStoresResource() {
        // Given
        String idempotencyKey = "complete-test-" + System.currentTimeMillis();
        String resourceId = "resource-123";

        // When
        Mono<IdempotencyService.IdempotencyResult> flow = idempotencyService.acquireLock(idempotencyKey)
                .flatMap(result -> {
                    if (result.status() == IdempotencyService.Status.NEW) {
                        return idempotencyService.completeProcessing(idempotencyKey, resourceId)
                                .thenReturn(result);
                    }
                    return Mono.just(result);
                });

        // Then
        StepVerifier.create(flow)
                .expectNextMatches(result -> result.status() == IdempotencyService.Status.NEW)
                .verifyComplete();

        // Verify stored resource
        StepVerifier.create(
                mongoTemplate.findById(idempotencyKey, IdempotencyService.IdempotencyRecord.class)
        )
                .expectNextMatches(record -> {
                    assertThat(record.getResourceId()).isEqualTo(resourceId);
                    assertThat(record.getStatus()).isEqualTo(IdempotencyService.IdempotencyRecord.Status.COMPLETED);
                    return true;
                })
                .verifyComplete();
    }
}
