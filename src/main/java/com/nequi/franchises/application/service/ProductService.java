package com.nequi.franchises.application.service;

import com.nequi.franchises.application.dto.CreateProductRequest;
import com.nequi.franchises.application.dto.ProductDto;
import com.nequi.franchises.application.dto.UpdateNameDto;
import com.nequi.franchises.application.dto.UpdateStockRequest;
import com.nequi.franchises.application.mapper.ProductMapper;
import com.nequi.franchises.domain.exception.BranchNotFoundException;
import com.nequi.franchises.domain.exception.ProductNotFoundException;
import com.nequi.franchises.domain.exception.ValidationException;
import com.nequi.franchises.domain.model.Product;
import com.nequi.franchises.domain.repository.ProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing products in branches.
 * 
 * DESIGN: Products are stored in a separate MongoDB collection with branch reference.
 * Branch validation ensures products are only created within valid franchise/branch hierarchy.
 * 
 * ARCHITECTURE DECISIONS:
 * - Creation requires franchise+branch context (nested endpoint)
 * - Operations on existing products use global ID for efficiency
 * - All mutations use explicit domain methods (updateStock, updateName)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final TransactionalOperator transactionalOperator;
    private final BranchService branchService;

    // ==================== Validation Helpers ====================
    
    private Mono<Void> validateFranchiseAndBranchIds(String franchiseId, String branchId) {
        if (isBlank(franchiseId)) return Mono.error(new ValidationException("Franchise ID is required"));
        if (isBlank(branchId)) return Mono.error(new ValidationException("Branch ID is required"));
        return Mono.empty();
    }
    
    private Mono<Void> validateProductId(String id) {
        if (isBlank(id)) return Mono.error(new ValidationException("Product ID is required"));
        return Mono.empty();
    }
    
    private boolean isBlank(String str) {
        return str == null || str.isBlank();
    }
    
    private Product buildProduct(String branchId, CreateProductRequest request) {
        return Product.builder()
                .id(UUID.randomUUID().toString())
                .branchId(branchId)
                .name(request.getName())
                .stock(request.getStock())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== Public API ====================

    /**
     * Create a product within a specific branch of a franchise.
     * Validates that the branch exists within the franchise before creation.
     */
    @CircuitBreaker(name = "productService")
    public Mono<ProductDto> createProduct(String franchiseId, String branchId, CreateProductRequest request) {
        if (request.getStock() < 0) {
            return Mono.error(new ValidationException("Stock cannot be negative"));
        }
        
        return validateFranchiseAndBranchIds(franchiseId, branchId)
                .then(Mono.defer(() -> branchService.branchExistsInFranchise(franchiseId, branchId)))
                .flatMap(exists -> exists 
                        ? productRepository.save(buildProduct(branchId, request))
                        : Mono.error(new BranchNotFoundException(branchId)))
                .doOnNext(product -> log.info("Created product '{}' in branch '{}' of franchise '{}'", 
                        product.getName(), branchId, franchiseId))
                .map(productMapper::toDto)
                .as(transactionalOperator::transactional);
    }

    @CircuitBreaker(name = "productService")
    public Mono<ProductDto> getProductById(String id) {
        if (isBlank(id)) {
            return Mono.error(new ValidationException("Product ID is required"));
        }
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)))
                .map(productMapper::toDto);
    }

    @CircuitBreaker(name = "productService")
    public Flux<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .map(productMapper::toDto);
    }

    /**
     * Get all products for a specific branch within a franchise.
     */
    @CircuitBreaker(name = "productService")
    public Flux<ProductDto> getProductsByBranchId(String franchiseId, String branchId) {
        return validateFranchiseAndBranchIds(franchiseId, branchId)
                .thenMany(branchService.branchExistsInFranchise(franchiseId, branchId)
                        .flatMapMany(exists -> exists
                                ? productRepository.findByBranchId(branchId).map(productMapper::toDto)
                                : Flux.error(new BranchNotFoundException(branchId))));
    }

    public Mono<Void> deleteProduct(String id) {
        return validateProductId(id)
                .then(Mono.defer(() -> productRepository.existsById(id)))
                .flatMap(exists -> exists 
                        ? productRepository.deleteById(id) 
                        : Mono.error(new ProductNotFoundException(id)))
                .as(transactionalOperator::transactional);
    }

    private Mono<Product> findProductByIdOrError(String id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new ProductNotFoundException(id)));
    }

    public Mono<ProductDto> updateProductStock(String id, UpdateStockRequest request) {
        if (request.getStock() < 0) {
            return Mono.error(new ValidationException("Stock cannot be negative"));
        }
        return validateProductId(id)
                .then(Mono.defer(() -> findProductByIdOrError(id)))
                .flatMap(product -> {
                    product.updateStock(request.getStock());
                    return productRepository.save(product);
                })
                .map(productMapper::toDto)
                .as(transactionalOperator::transactional);
    }

    public Mono<ProductDto> updateProductName(String id, UpdateNameDto request) {
        if (isBlank(request.getName())) {
            return Mono.error(new ValidationException("Product name cannot be empty"));
        }
        return validateProductId(id)
                .then(Mono.defer(() -> findProductByIdOrError(id)))
                .flatMap(product -> {
                    product.updateName(request.getName());
                    return productRepository.save(product);
                })
                .map(productMapper::toDto)
                .as(transactionalOperator::transactional);
    }
}
