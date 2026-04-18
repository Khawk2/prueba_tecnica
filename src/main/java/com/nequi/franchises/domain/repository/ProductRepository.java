package com.nequi.franchises.domain.repository;

import com.nequi.franchises.domain.model.Product;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Reactive MongoDB Repository for Product.
 * Products are stored in a separate collection with branch reference.
 */
@Repository
public interface ProductRepository extends ReactiveMongoRepository<Product, String> {

    /**
     * Find all products by branch ID.
     * Uses index on branch_id field.
     */
    Flux<Product> findByBranchId(String branchId);

    /**
     * Find all products by branch IDs (batch query).
     * Optimized for fetching products across multiple branches in one query.
     * Uses MongoDB $in operator.
     */
    @Query("{ 'branch_id': { $in: ?0 } }")
    Flux<Product> findByBranchIdIn(List<String> branchIds);

    /**
     * Find top product by branch ID ordered by stock descending.
     * Uses compound index on branch_id + stock.
     */
    @Query(value = "{ 'branch_id': ?0 }", sort = "{ 'stock': -1 }")
    Mono<Product> findTopByBranchIdOrderByStockDesc(String branchId);
}
