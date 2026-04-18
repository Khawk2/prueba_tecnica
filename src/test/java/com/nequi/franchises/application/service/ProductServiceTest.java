package com.nequi.franchises.application.service;

import com.nequi.franchises.application.dto.CreateProductRequest;
import com.nequi.franchises.application.dto.ProductDto;
import com.nequi.franchises.application.dto.UpdateNameDto;
import com.nequi.franchises.application.dto.UpdateStockRequest;
import com.nequi.franchises.application.mapper.ProductMapper;
import com.nequi.franchises.domain.exception.ProductNotFoundException;
import com.nequi.franchises.domain.exception.ValidationException;
import com.nequi.franchises.domain.model.Product;
import com.nequi.franchises.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

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

    private Product sampleProduct;
    private ProductDto sampleProductDto;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id("prod-123")
                .branchId("branch-456")
                .name("Test Product")
                .stock(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleProductDto = ProductDto.builder()
                .id("prod-123")
                .branchId("branch-456")
                .name("Test Product")
                .stock(100)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Mock TransactionalOperator to pass-through
        lenient().doAnswer(invocation -> invocation.getArgument(0))
                .when(transactionalOperator).transactional(any(Mono.class));
        lenient().doAnswer(invocation -> invocation.getArgument(0))
                .when(transactionalOperator).transactional(any(Flux.class));
    }

    @Test
    @DisplayName("createProduct - should create product successfully")
    void createProduct_Success() {
        // Given
        String franchiseId = "franchise-123";
        String branchId = "branch-456";
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .stock(100)
                .build();

        when(branchService.branchExistsInFranchise(franchiseId, branchId)).thenReturn(Mono.just(true));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(sampleProduct));
        when(productMapper.toDto(sampleProduct)).thenReturn(sampleProductDto);

        // When & Then
        StepVerifier.create(productService.createProduct(franchiseId, branchId, request))
                .expectNextMatches(dto -> dto.getName().equals("Test Product") && dto.getStock() == 100)
                .verifyComplete();
    }

    @Test
    @DisplayName("createProduct - should error when stock is negative")
    void createProduct_NegativeStock_Error() {
        // Given
        String franchiseId = "franchise-123";
        String branchId = "branch-456";
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .stock(-10)
                .build();

        // When & Then
        StepVerifier.create(productService.createProduct(franchiseId, branchId, request))
                .expectError(ValidationException.class)
                .verify();

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("createProduct - should error when branchId is blank")
    void createProduct_BlankBranchId_Error() {
        // Given
        String franchiseId = "franchise-123";
        String branchId = "";
        CreateProductRequest request = CreateProductRequest.builder()
                .name("Test Product")
                .stock(100)
                .build();

        // When & Then
        StepVerifier.create(productService.createProduct(franchiseId, branchId, request))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("getProductById - should return product")
    void getProductById_Success() {
        // Given
        String id = "prod-123";

        when(productRepository.findById(id)).thenReturn(Mono.just(sampleProduct));
        when(productMapper.toDto(sampleProduct)).thenReturn(sampleProductDto);

        // When & Then
        StepVerifier.create(productService.getProductById(id))
                .expectNext(sampleProductDto)
                .verifyComplete();
    }

    @Test
    @DisplayName("getProductById - should error when not found")
    void getProductById_NotFound_Error() {
        // Given
        String id = "nonexistent";

        when(productRepository.findById(id)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(productService.getProductById(id))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("getProductById - should error when id is blank")
    void getProductById_BlankId_Error() {
        // When & Then
        StepVerifier.create(productService.getProductById(""))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("getAllProducts - should return all products")
    void getAllProducts_Success() {
        // Given
        when(productRepository.findAll()).thenReturn(Flux.just(sampleProduct));
        when(productMapper.toDto(sampleProduct)).thenReturn(sampleProductDto);

        // When & Then
        StepVerifier.create(productService.getAllProducts())
                .expectNext(sampleProductDto)
                .verifyComplete();
    }

    @Test
    @DisplayName("getProductsByBranchId - should return products for branch")
    void getProductsByBranchId_Success() {
        // Given
        String franchiseId = "franchise-123";
        String branchId = "branch-456";

        when(branchService.branchExistsInFranchise(franchiseId, branchId)).thenReturn(Mono.just(true));
        when(productRepository.findByBranchId(branchId)).thenReturn(Flux.just(sampleProduct));
        when(productMapper.toDto(sampleProduct)).thenReturn(sampleProductDto);

        // When & Then
        StepVerifier.create(productService.getProductsByBranchId(franchiseId, branchId))
                .expectNext(sampleProductDto)
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteProduct - should delete successfully")
    void deleteProduct_Success() {
        // Given
        String id = "prod-123";

        when(productRepository.existsById(id)).thenReturn(Mono.just(true));
        when(productRepository.deleteById(id)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(productService.deleteProduct(id))
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteProduct - should error when not found")
    void deleteProduct_NotFound_Error() {
        // Given
        String id = "nonexistent";

        when(productRepository.existsById(id)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(productService.deleteProduct(id))
                .expectError(ProductNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("updateProductStock - should update stock successfully")
    void updateProductStock_Success() {
        // Given
        String id = "prod-123";
        UpdateStockRequest request = new UpdateStockRequest();
        request.setStock(200);

        Product updatedProduct = Product.builder()
                .id("prod-123")
                .branchId("branch-456")
                .name("Test Product")
                .stock(200)
                .build();

        when(productRepository.findById(id)).thenReturn(Mono.just(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(updatedProduct));
        when(productMapper.toDto(any())).thenReturn(ProductDto.builder()
                .id("prod-123")
                .stock(200)
                .build());

        // When & Then
        StepVerifier.create(productService.updateProductStock(id, request))
                .expectNextMatches(dto -> dto.getStock() == 200)
                .verifyComplete();
    }

    @Test
    @DisplayName("updateProductStock - should error when stock is negative")
    void updateProductStock_NegativeStock_Error() {
        // Given
        String id = "prod-123";
        UpdateStockRequest request = new UpdateStockRequest();
        request.setStock(-50);

        // When & Then
        StepVerifier.create(productService.updateProductStock(id, request))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("updateProductName - should update name successfully")
    void updateProductName_Success() {
        // Given
        String id = "prod-123";
        UpdateNameDto request = new UpdateNameDto();
        request.setName("Updated Product Name");

        Product updatedProduct = Product.builder()
                .id("prod-123")
                .branchId("branch-456")
                .name("Updated Product Name")
                .stock(100)
                .build();

        when(productRepository.findById(id)).thenReturn(Mono.just(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(Mono.just(updatedProduct));
        when(productMapper.toDto(any())).thenReturn(ProductDto.builder()
                .id("prod-123")
                .name("Updated Product Name")
                .build());

        // When & Then
        StepVerifier.create(productService.updateProductName(id, request))
                .expectNextMatches(dto -> dto.getName().equals("Updated Product Name"))
                .verifyComplete();
    }
}
