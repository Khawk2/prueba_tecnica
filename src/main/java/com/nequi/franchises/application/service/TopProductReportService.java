package com.nequi.franchises.application.service;

import com.nequi.franchises.application.dto.TopProductDto;
import com.nequi.franchises.application.mapper.ProductMapper;
import com.nequi.franchises.domain.exception.FranchiseNotFoundException;
import com.nequi.franchises.domain.exception.ValidationException;
import com.nequi.franchises.domain.model.Branch;
import com.nequi.franchises.domain.model.Franchise;
import com.nequi.franchises.domain.model.Product;
import com.nequi.franchises.domain.repository.FranchiseRepository;
import com.nequi.franchises.domain.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for generating top product reports across franchises.
 * 
 * DESIGN DECISION: In MongoDB, branches are embedded in Franchise documents.
 * This service optimizes queries by:
 * 1. Loading franchise with embedded branches (single document)
 * 2. Extracting branch IDs
 * 3. Querying products with $in operator for all branches at once
 * 4. Aggregating top products per branch in memory
 * 
 * This approach avoids N+1 queries and leverages MongoDB's document model.
 */
@Service
@RequiredArgsConstructor
public class TopProductReportService {

    private final FranchiseRepository franchiseRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    /**
     * Gets the product with most stock for each branch in a franchise.
     * 
     * ALGORITHM:
     * 1. Load franchise with embedded branches (1 document)
     * 2. Extract branch IDs from embedded array
     * 3. Query all products for these branches in single query ($in)
     * 4. Group by branch and find top product per branch
     * 
     * @param franchiseId the franchise ID (MongoDB ObjectId as String)
     * @return flux of top products per branch
     */
    @CircuitBreaker(name = "productService")
    public Flux<TopProductDto> getTopProductsByFranchise(String franchiseId) {
        if (franchiseId == null || franchiseId.isBlank()) {
            return Flux.error(new ValidationException("Franchise ID is required"));
        }
        
        return franchiseRepository.findByIdWithBranches(franchiseId)
                .switchIfEmpty(Mono.error(new FranchiseNotFoundException(franchiseId)))
                .flatMapMany(this::processFranchiseProducts);
    }

    /**
     * Process products for all branches in a franchise.
     * Optimized to use single query with $in operator.
     */
    private Flux<TopProductDto> processFranchiseProducts(Franchise franchise) {
        List<Branch> branches = franchise.getBranches();
        
        if (branches == null || branches.isEmpty()) {
            return Flux.empty();
        }

        // Map branch IDs to names for lookup
        Map<String, String> branchNameMap = branches.stream()
                .filter(b -> b.getId() != null)
                .collect(Collectors.toMap(Branch::getId, Branch::getName));

        List<String> branchIds = branches.stream()
                .map(Branch::getId)
                .collect(Collectors.toList());

        // Single query for all products in all branches
        return productRepository.findByBranchIdIn(branchIds)
                .collectList()
                .flatMapMany(products -> aggregateTopProductsPerBranch(products, branchNameMap));
    }

    /**
     * Aggregate top products per branch from list.
     * Groups by branchId and finds product with max stock for each.
     */
    private Flux<TopProductDto> aggregateTopProductsPerBranch(
            List<Product> products,
            Map<String, String> branchNameMap) {
        
        // Group by branch and find top product per branch
        Map<String, Product> topProductsByBranch = products.stream()
                .collect(Collectors.groupingBy(
                        Product::getBranchId,
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparingInt(Product::getStock)),
                                optional -> optional.orElse(null)
                        )
                ));

        return Flux.fromIterable(topProductsByBranch.entrySet())
                .filter(entry -> entry.getValue() != null)
                .map(entry -> {
                    String branchId = entry.getKey();
                    Product product = entry.getValue();
                    String branchName = branchNameMap.getOrDefault(branchId, "Unknown Branch");
                    return productMapper.toTopProductDto(product, branchName);
                });
    }
}
