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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Edge case tests for ProductService.
 * Following AAA pattern: Arrange, Act, Assert
 * Testing boundary conditions and error scenarios.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceEdgeCasesTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private TransactionalOperator transactionalOperator;

    @Mock
    private BranchService branchService;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        // Mock TransactionalOperator to pass-through
        lenient().doAnswer(invocation -> invocation.getArgument(0))
                .when(transactionalOperator).transactional(any(Mono.class));
        lenient().doAnswer(invocation -> invocation.getArgument(0))
                .when(transactionalOperator).transactional(any(Flux.class));
    }

    @Nested
    @DisplayName("Create Product - Edge Cases")
    class CreateProductEdgeCases {

        @Test
        @DisplayName("Should reject when franchiseId is null")
        void createProduct_NullFranchiseId_ShouldReject() {
            // Arrange
            String franchiseId = null;
            String branchId = "branch-456";
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Test Product")
                    .stock(100)
                    .build();

            // Act & Assert
            StepVerifier.create(productService.createProduct(franchiseId, branchId, request))
                    .expectError(ValidationException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should reject when branchId is null")
        void createProduct_NullBranchId_ShouldReject() {
            // Arrange
            String franchiseId = "franchise-123";
            String branchId = null;
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Test Product")
                    .stock(100)
                    .build();

            // Act & Assert
            StepVerifier.create(productService.createProduct(franchiseId, branchId, request))
                    .expectError(ValidationException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should reject when franchiseId is empty string")
        void createProduct_EmptyFranchiseId_ShouldReject() {
            // Arrange
            String franchiseId = "   ";
            String branchId = "branch-456";
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Test Product")
                    .stock(100)
                    .build();

            // Act & Assert
            StepVerifier.create(productService.createProduct(franchiseId, branchId, request))
                    .expectError(ValidationException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should reject when branch does not exist in franchise")
        void createProduct_BranchNotInFranchise_ShouldReject() {
            // Arrange
            String franchiseId = "franchise-123";
            String branchId = "invalid-branch";
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Test Product")
                    .stock(100)
                    .build();

            when(branchService.branchExistsInFranchise(franchiseId, branchId))
                    .thenReturn(Mono.just(false));

            // Act & Assert
            StepVerifier.create(productService.createProduct(franchiseId, branchId, request))
                    .expectError(BranchNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should reject when stock is zero (if business requires positive)")
        void createProduct_ZeroStock_ShouldAccept() {
            // Arrange - Note: Zero stock might be valid (out of stock product)
            String franchiseId = "franchise-123";
            String branchId = "branch-456";
            CreateProductRequest request = CreateProductRequest.builder()
                    .name("Out of Stock Product")
                    .stock(0)
                    .build();

            Product expectedProduct = Product.builder()
                    .id("prod-789")
                    .branchId(branchId)
                    .name("Out of Stock Product")
                    .stock(0)
                    .build();

            ProductDto expectedDto = ProductDto.builder()
                    .id("prod-789")
                    .stock(0)
                    .build();

            when(branchService.branchExistsInFranchise(franchiseId, branchId))
                    .thenReturn(Mono.just(true));
            when(productRepository.save(any(Product.class)))
                    .thenReturn(Mono.just(expectedProduct));
            when(productMapper.toDto(expectedProduct))
                    .thenReturn(expectedDto);

            // Act & Assert
            StepVerifier.create(productService.createProduct(franchiseId, branchId, request))
                    .expectNextMatches(dto -> dto.getStock() == 0)
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Get Product By ID - Edge Cases")
    class GetProductByIdEdgeCases {

        @Test
        @DisplayName("Should return error when product not found")
        void getProductById_NotFound_ShouldReturnError() {
            // Arrange
            String productId = "non-existent-id";
            when(productRepository.findById(productId))
                    .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(productService.getProductById(productId))
                    .expectError(ProductNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should reject null product ID")
        void getProductById_NullId_ShouldReject() {
            // Arrange
            String productId = null;

            // Act & Assert
            StepVerifier.create(productService.getProductById(productId))
                    .expectError(ValidationException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Get Products By Branch - Edge Cases")
    class GetProductsByBranchEdgeCases {

        @Test
        @DisplayName("Should return empty flux when branch has no products")
        void getProductsByBranch_EmptyBranch_ShouldReturnEmpty() {
            // Arrange
            String franchiseId = "franchise-123";
            String branchId = "branch-empty";

            when(branchService.branchExistsInFranchise(franchiseId, branchId))
                    .thenReturn(Mono.just(true));
            when(productRepository.findByBranchId(branchId))
                    .thenReturn(Flux.empty());

            // Act & Assert
            StepVerifier.create(productService.getProductsByBranchId(franchiseId, branchId))
                    .expectNextCount(0)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should reject when branch does not exist in franchise")
        void getProductsByBranch_InvalidBranch_ShouldReject() {
            // Arrange
            String franchiseId = "franchise-123";
            String branchId = "invalid-branch";

            when(branchService.branchExistsInFranchise(franchiseId, branchId))
                    .thenReturn(Mono.just(false));

            // Act & Assert
            StepVerifier.create(productService.getProductsByBranchId(franchiseId, branchId))
                    .expectError(BranchNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should reject when franchise does not exist")
        void getProductsByBranch_InvalidFranchise_ShouldReject() {
            // Arrange
            String franchiseId = "invalid-franchise";
            String branchId = "branch-456";

            when(branchService.branchExistsInFranchise(franchiseId, branchId))
                    .thenReturn(Mono.just(false));

            // Act & Assert
            StepVerifier.create(productService.getProductsByBranchId(franchiseId, branchId))
                    .expectError(BranchNotFoundException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Update Product - Edge Cases")
    class UpdateProductEdgeCases {

        @Test
        @DisplayName("Update stock - should reject when product not found")
        void updateStock_ProductNotFound_ShouldReject() {
            // Arrange
            String productId = "non-existent";
            UpdateStockRequest request = new UpdateStockRequest();
            request.setStock(50);

            when(productRepository.findById(productId))
                    .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(productService.updateProductStock(productId, request))
                    .expectError(ProductNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("Update name - should reject null name")
        void updateName_NullName_ShouldReject() {
            // Arrange
            String productId = "prod-123";
            UpdateNameDto request = new UpdateNameDto();
            request.setName(null);

            // Act & Assert
            StepVerifier.create(productService.updateProductName(productId, request))
                    .expectError(ValidationException.class)
                    .verify();
        }

        @Test
        @DisplayName("Update name - should reject empty name with spaces")
        void updateName_EmptyNameWithSpaces_ShouldReject() {
            // Arrange
            String productId = "prod-123";
            UpdateNameDto request = new UpdateNameDto();
            request.setName("   ");

            // Act & Assert
            StepVerifier.create(productService.updateProductName(productId, request))
                    .expectError(ValidationException.class)
                    .verify();
        }
    }

    @Nested
    @DisplayName("Delete Product - Edge Cases")
    class DeleteProductEdgeCases {

        @Test
        @DisplayName("Should reject when product not found")
        void deleteProduct_NotFound_ShouldReject() {
            // Arrange
            String productId = "non-existent";
            when(productRepository.existsById(productId))
                    .thenReturn(Mono.just(false));

            // Act & Assert
            StepVerifier.create(productService.deleteProduct(productId))
                    .expectError(ProductNotFoundException.class)
                    .verify();
        }

        @Test
        @DisplayName("Should succeed when product exists")
        void deleteProduct_Exists_ShouldSucceed() {
            // Arrange
            String productId = "prod-123";
            when(productRepository.existsById(productId))
                    .thenReturn(Mono.just(true));
            when(productRepository.deleteById(productId))
                    .thenReturn(Mono.empty());

            // Act & Assert
            StepVerifier.create(productService.deleteProduct(productId))
                    .verifyComplete();
        }
    }
}
