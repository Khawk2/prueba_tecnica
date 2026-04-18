package com.nequi.franchises.infrastructure.service;

import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Idempotency Service with WRITE-FIRST pattern.
 * 
 * CRITICAL: This implementation prevents race conditions by using MongoDB's unique index.
 * Pattern: Try INSERT first → if duplicate key exception, request already processed.
 * This is ATOMIC - no check-then-act race condition possible.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ReactiveMongoTemplate mongoTemplate;

    /**
     * Idempotency status for request processing.
     */
    public enum Status {
        NEW,        // Key inserted, should process request
        ALREADY_PROCESSED,  // Key exists, request already processed
        ERROR       // Unexpected error
    }

    public record IdempotencyResult(Status status, String resourceId) {}

    /**
     * Try to acquire idempotency lock.
     * 
     * PATTERN: WRITE-FIRST (atomic insert with unique index)
     * 1. Try to insert key with status=PROCESSING
     * 2. If success (inserted) → proceed with request
     * 3. If duplicate key → already processed, return existing resource
     * 4. After processing, update to status=COMPLETED with resourceId
     * 
     * @param idempotencyKey Unique key for the request
     * @return Mono<IdempotencyResult> indicating whether to process or not
     */
    public Mono<IdempotencyResult> acquireLock(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.just(new IdempotencyResult(Status.NEW, null));
        }

        IdempotencyRecord record = IdempotencyRecord.builder()
                .id(idempotencyKey)
                .status(IdempotencyRecord.Status.PROCESSING)
                .createdAt(Instant.now())
                .build();

        return mongoTemplate.insert(record)
                .map(r -> new IdempotencyResult(Status.NEW, null))
                .onErrorResume(DuplicateKeyException.class, e -> {
                    // Key already exists - request was already processed
                    log.info("Idempotency key already exists: {}", idempotencyKey);
                    return mongoTemplate.findById(idempotencyKey, IdempotencyRecord.class)
                            .map(existing -> new IdempotencyResult(
                                    Status.ALREADY_PROCESSED, 
                                    existing.getResourceId()))
                            .defaultIfEmpty(new IdempotencyResult(Status.ALREADY_PROCESSED, null));
                })
                .onErrorResume(e -> {
                    log.error("Error acquiring idempotency lock: {}", idempotencyKey, e);
                    return Mono.just(new IdempotencyResult(Status.ERROR, null));
                });
    }

    /**
     * Complete processing and store result.
     * Called after successful request processing.
     */
    public Mono<Void> completeProcessing(String idempotencyKey, String resourceId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Mono.empty();
        }

        return mongoTemplate.findById(idempotencyKey, IdempotencyRecord.class)
                .flatMap(record -> {
                    record.setStatus(IdempotencyRecord.Status.COMPLETED);
                    record.setResourceId(resourceId);
                    record.setCompletedAt(Instant.now());
                    return mongoTemplate.save(record);
                })
                .doOnSuccess(r -> log.info("Idempotency completed: {} -> {}", idempotencyKey, resourceId))
                .then();
    }

    /**
     * Record for idempotency tracking with unique index on id.
     * TTL index should be created on createdAt for automatic cleanup.
     */
    @org.springframework.data.mongodb.core.mapping.Document(collection = "idempotency_keys")
    @lombok.Builder
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class IdempotencyRecord {
        @org.springframework.data.annotation.Id
        private String id;
        private Status status;
        private String resourceId;
        private Instant createdAt;
        private Instant completedAt;

        public enum Status {
            PROCESSING,   // Currently being processed
            COMPLETED,    // Successfully completed
            FAILED        // Processing failed
        }
    }
}
