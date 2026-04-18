package com.nequi.franchises.infrastructure.controller;

import com.nequi.franchises.application.dto.CreateProductRequest;
import com.nequi.franchises.application.dto.ProductDto;
import com.nequi.franchises.application.dto.UpdateNameDto;
import com.nequi.franchises.application.dto.UpdateStockRequest;
import com.nequi.franchises.application.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller for product operations.
 * 
 * DESIGN: Products are nested under franchises/branches to reflect the domain relationship.
 * All product operations are contextual to a franchise and branch.
 * 
 * RESTful routes:
 * - POST   /api/v1/franchises/{franchiseId}/branches/{branchId}/products  -> Create
 * - GET    /api/v1/franchises/{franchiseId}/branches/{branchId}/products  -> List by branch
 * - DELETE /api/v1/products/{id}                                         -> Delete
 * - PATCH  /api/v1/products/{id}/stock                                   -> Update stock
 * - PATCH  /api/v1/products/{id}/name                                    -> Update name
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Products", description = "Gestión de productos - Crear, actualizar stock, eliminar productos")
public class ProductController {

    private final ProductService productService;

    /**
     * Create a new product within a specific branch.
     * The branchId and franchiseId come from the URL path for REST consistency.
     */
    @PostMapping("/franchises/{franchiseId}/branches/{branchId}/products")
    public Mono<ResponseEntity<ProductDto>> createProduct(
            @PathVariable String franchiseId,
            @PathVariable String branchId,
            @Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(franchiseId, branchId, request)
                .map(product -> ResponseEntity.status(HttpStatus.CREATED).body(product));
    }

    /**
     * Get all products for a specific branch.
     */
    @GetMapping("/franchises/{franchiseId}/branches/{branchId}/products")
    public Flux<ProductDto> getProductsByBranch(
            @PathVariable String franchiseId,
            @PathVariable String branchId) {
        return productService.getProductsByBranchId(franchiseId, branchId);
    }

    /**
     * Get a specific product by ID (global lookup).
     */
    @GetMapping("/products/{id}")
    public Mono<ResponseEntity<ProductDto>> getProductById(@PathVariable String id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok);
    }

    /**
     * Get all products across all branches (global listing).
     */
    @GetMapping("/products")
    public Flux<ProductDto> getAllProducts() {
        return productService.getAllProducts();
    }

    /**
     * Delete a product by ID.
     */
    @DeleteMapping("/products/{id}")
    public Mono<ResponseEntity<Void>> deleteProduct(@PathVariable String id) {
        return productService.deleteProduct(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    /**
     * Update product stock.
     */
    @PatchMapping("/products/{id}/stock")
    public Mono<ResponseEntity<ProductDto>> updateProductStock(
            @PathVariable String id,
            @Valid @RequestBody UpdateStockRequest request) {
        return productService.updateProductStock(id, request)
                .map(ResponseEntity::ok);
    }

    /**
     * Update product name.
     */
    @PatchMapping("/products/{id}/name")
    public Mono<ResponseEntity<ProductDto>> updateProductName(
            @PathVariable String id,
            @Valid @RequestBody UpdateNameDto request) {
        return productService.updateProductName(id, request)
                .map(ResponseEntity::ok);
    }
}
