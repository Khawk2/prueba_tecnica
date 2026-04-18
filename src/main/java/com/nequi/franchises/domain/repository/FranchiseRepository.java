package com.nequi.franchises.domain.repository;

import com.nequi.franchises.domain.model.Franchise;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive MongoDB Repository for Franchise.
 * Extends ReactiveMongoRepository for basic CRUD operations.
 */
@Repository
public interface FranchiseRepository extends ReactiveMongoRepository<Franchise, String> {

    /**
     * Find franchise by ID with embedded branches populated.
     * In MongoDB, branches are embedded so this is just findById.
     */
    @Query("{ '_id': ?0 }")
    Mono<Franchise> findByIdWithBranches(String id);

    /**
     * Update franchise name by ID.
     * Note: For reactive MongoDB, updates are done via find-modify-save pattern
     * in the service layer to maintain domain integrity and publish events.
     */

    /**
     * Paginated find all franchises.
     * Prevents loading large collections in memory.
     */
    Flux<Franchise> findAllBy(Pageable pageable);

    /**
     * Check if franchise exists by name (for uniqueness validation).
     */
    Mono<Boolean> existsByName(String name);
}
