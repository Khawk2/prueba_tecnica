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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopProductReportServiceTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private TopProductReportService topProductReportService;

    private Franchise sampleFranchise;
    private List<Product> sampleProducts;

    @BeforeEach
    void setUp() {
        // Setup branches
        Branch branch1 = Branch.builder()
                .id("branch-1")
                .name("Branch One")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Branch branch2 = Branch.builder()
                .id("branch-2")
                .name("Branch Two")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<Branch> branches = Arrays.asList(branch1, branch2);

        // Setup franchise with branches
        sampleFranchise = Franchise.builder()
                .id("franchise-123")
                .name("Test Franchise")
                .branches(branches)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup products
        Product product1 = Product.builder()
                .id("prod-1")
                .branchId("branch-1")
                .name("Product A")
                .stock(150)
                .build();

        Product product2 = Product.builder()
                .id("prod-2")
                .branchId("branch-1")
                .name("Product B")
                .stock(50)
                .build();

        Product product3 = Product.builder()
                .id("prod-3")
                .branchId("branch-2")
                .name("Product C")
                .stock(200)
                .build();

        sampleProducts = Arrays.asList(product1, product2, product3);
    }

    @Test
    @DisplayName("getTopProductsByFranchise - should return top product per branch")
    void getTopProductsByFranchise_Success() {
        // Given
        String franchiseId = "franchise-123";

        when(franchiseRepository.findByIdWithBranches(franchiseId)).thenReturn(Mono.just(sampleFranchise));
        when(productRepository.findByBranchIdIn(anyList())).thenReturn(Flux.fromIterable(sampleProducts));
        when(productMapper.toTopProductDto(any(Product.class), anyString()))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    String branchName = invocation.getArgument(1);
                    return TopProductDto.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .stock(p.getStock())
                            .branchId(p.getBranchId())
                            .branchName(branchName)
                            .build();
                });

        // When & Then
        StepVerifier.create(topProductReportService.getTopProductsByFranchise(franchiseId))
                .expectNextCount(2) // One top product per branch
                .verifyComplete();
    }

    @Test
    @DisplayName("getTopProductsByFranchise - should return highest stock product per branch")
    void getTopProductsByFranchise_CorrectTopProduct() {
        // Given
        String franchiseId = "franchise-123";

        when(franchiseRepository.findByIdWithBranches(franchiseId)).thenReturn(Mono.just(sampleFranchise));
        when(productRepository.findByBranchIdIn(anyList())).thenReturn(Flux.fromIterable(sampleProducts));
        when(productMapper.toTopProductDto(any(Product.class), anyString()))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    String branchName = invocation.getArgument(1);
                    return TopProductDto.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .stock(p.getStock())
                            .branchId(p.getBranchId())
                            .branchName(branchName)
                            .build();
                });

        // When & Then - Verify we get product with highest stock per branch
        StepVerifier.create(topProductReportService.getTopProductsByFranchise(franchiseId))
                .expectNextMatches(dto -> dto.getBranchId().equals("branch-1") && dto.getStock() == 150)
                .expectNextMatches(dto -> dto.getBranchId().equals("branch-2") && dto.getStock() == 200)
                .verifyComplete();
    }

    @Test
    @DisplayName("getTopProductsByFranchise - should error when franchise not found")
    void getTopProductsByFranchise_NotFound_Error() {
        // Given
        String franchiseId = "nonexistent";

        when(franchiseRepository.findByIdWithBranches(franchiseId)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(topProductReportService.getTopProductsByFranchise(franchiseId))
                .expectError(FranchiseNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("getTopProductsByFranchise - should error when id is blank")
    void getTopProductsByFranchise_BlankId_Error() {
        // When & Then
        StepVerifier.create(topProductReportService.getTopProductsByFranchise(""))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("getTopProductsByFranchise - should return empty when no branches")
    void getTopProductsByFranchise_NoBranches_Empty() {
        // Given
        String franchiseId = "franchise-123";
        Franchise franchiseWithoutBranches = Franchise.builder()
                .id("franchise-123")
                .name("Empty Franchise")
                .branches(new ArrayList<>())
                .build();

        when(franchiseRepository.findByIdWithBranches(franchiseId)).thenReturn(Mono.just(franchiseWithoutBranches));

        // When & Then
        StepVerifier.create(topProductReportService.getTopProductsByFranchise(franchiseId))
                .verifyComplete();
    }

    @Test
    @DisplayName("getTopProductsByFranchise - should return empty when no products")
    void getTopProductsByFranchise_NoProducts_Empty() {
        // Given
        String franchiseId = "franchise-123";

        when(franchiseRepository.findByIdWithBranches(franchiseId)).thenReturn(Mono.just(sampleFranchise));
        when(productRepository.findByBranchIdIn(anyList())).thenReturn(Flux.empty());

        // When & Then
        StepVerifier.create(topProductReportService.getTopProductsByFranchise(franchiseId))
                .verifyComplete();
    }
}
