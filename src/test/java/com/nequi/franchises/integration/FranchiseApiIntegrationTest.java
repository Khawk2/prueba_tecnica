package com.nequi.franchises.integration;

import com.nequi.franchises.application.dto.CreateFranchiseRequest;
import com.nequi.franchises.application.dto.CreateBranchRequest;
import com.nequi.franchises.application.dto.CreateProductRequest;
import com.nequi.franchises.application.dto.FranchiseDto;
import com.nequi.franchises.application.dto.BranchDto;
import com.nequi.franchises.application.dto.ProductDto;
import com.nequi.franchises.application.dto.UpdateStockRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Franchise API using WebTestClient.
 * Tests complete REST API contracts with real HTTP requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class FranchiseApiIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final String BASE_URL = "/api/v1";

    @BeforeEach
    void setUp() {
        // WebTestClient is configured automatically by @AutoConfigureWebTestClient
    }

    @Test
    @DisplayName("POST /franchises - should create franchise and return 201")
    void createFranchise_Success() {
        // Given
        CreateFranchiseRequest request = CreateFranchiseRequest.builder()
                .name("Integration Test Franchise")
                .build();

        // When & Then
        webTestClient.post()
                .uri(BASE_URL + "/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), CreateFranchiseRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(FranchiseDto.class)
                .value(dto -> {
                    assertThat(dto.getName()).isEqualTo("Integration Test Franchise");
                    assertThat(dto.getId()).isNotNull();
                    assertThat(dto.getCreatedAt()).isNotNull();
                });
    }

    @Test
    @DisplayName("POST /franchises - should return 400 when name is blank")
    void createFranchise_BlankName_BadRequest() {
        // Given
        CreateFranchiseRequest request = CreateFranchiseRequest.builder()
                .name("")
                .build();

        // When & Then
        webTestClient.post()
                .uri(BASE_URL + "/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), CreateFranchiseRequest.class)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("GET /franchises - should return list of franchises")
    void getAllFranchises_Success() {
        // When & Then
        webTestClient.get()
                .uri(BASE_URL + "/franchises")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(FranchiseDto.class);
    }

    @Test
    @DisplayName("Complete flow: Create Franchise -> Branch -> Product")
    void completeFlow_FranchiseBranchProduct() {
        // Step 1: Create Franchise
        CreateFranchiseRequest franchiseRequest = CreateFranchiseRequest.builder()
                .name("Flow Test Franchise")
                .build();

        FranchiseDto createdFranchise = webTestClient.post()
                .uri(BASE_URL + "/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(franchiseRequest), CreateFranchiseRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(createdFranchise).isNotNull();
        assertThat(createdFranchise.getId()).isNotNull();
        String franchiseId = createdFranchise.getId();

        // Step 2: Create Branch
        CreateBranchRequest branchRequest = CreateBranchRequest.builder()
                .name("Flow Test Branch")
                .build();

        BranchDto createdBranch = webTestClient.post()
                .uri(BASE_URL + "/franchises/{franchiseId}/branches", franchiseId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(branchRequest), CreateBranchRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(createdBranch).isNotNull();
        assertThat(createdBranch.getId()).isNotNull();
        String branchId = createdBranch.getId();

        // Step 3: Create Product
        CreateProductRequest productRequest = CreateProductRequest.builder()
                .name("Flow Test Product")
                .stock(50)
                .build();

        ProductDto createdProduct = webTestClient.post()
                .uri(BASE_URL + "/franchises/{franchiseId}/branches/{branchId}/products", franchiseId, branchId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(productRequest), CreateProductRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(createdProduct).isNotNull();
        assertThat(createdProduct.getId()).isNotNull();
        assertThat(createdProduct.getBranchId()).isEqualTo(branchId);
        assertThat(createdProduct.getName()).isEqualTo("Flow Test Product");
        assertThat(createdProduct.getStock()).isEqualTo(50);
        String productId = createdProduct.getId();

        // Step 4: Update Product Stock
        UpdateStockRequest stockRequest = new UpdateStockRequest();
        stockRequest.setStock(100);

        webTestClient.patch()
                .uri(BASE_URL + "/products/{productId}/stock", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(stockRequest), UpdateStockRequest.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProductDto.class)
                .value(updatedProduct -> {
                    assertThat(updatedProduct.getStock()).isEqualTo(100);
                    assertThat(updatedProduct.getId()).isEqualTo(productId);
                });

        // Step 5: Get Products by Branch
        webTestClient.get()
                .uri(BASE_URL + "/franchises/{franchiseId}/branches/{branchId}/products", franchiseId, branchId)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProductDto.class)
                .hasSize(1)
                .value(products -> {
                    assertThat(products.get(0).getName()).isEqualTo("Flow Test Product");
                    assertThat(products.get(0).getStock()).isEqualTo(100);
                });

        // Step 6: Delete Product
        webTestClient.delete()
                .uri(BASE_URL + "/products/{productId}", productId)
                .exchange()
                .expectStatus().isNoContent();

        // Step 7: Verify product is deleted (should return 404)
        webTestClient.get()
                .uri(BASE_URL + "/products/{productId}", productId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /franchises/{id} - should return 404 for non-existent franchise")
    void getFranchiseById_NotFound() {
        webTestClient.get()
                .uri(BASE_URL + "/franchises/{id}", "non-existent-id-12345")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("PATCH /franchises/{id}/name - should update franchise name")
    void updateFranchiseName_Success() {
        // First create a franchise
        CreateFranchiseRequest createRequest = CreateFranchiseRequest.builder()
                .name("Original Name")
                .build();

        FranchiseDto created = webTestClient.post()
                .uri(BASE_URL + "/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(createRequest), CreateFranchiseRequest.class)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(created).isNotNull();

        // Update the name
        var updateRequest = new com.nequi.franchises.application.dto.UpdateNameDto();
        updateRequest.setName("Updated Name");

        webTestClient.patch()
                .uri(BASE_URL + "/franchises/{id}/name", created.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(updateRequest), com.nequi.franchises.application.dto.UpdateNameDto.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody(FranchiseDto.class)
                .value(updated -> assertThat(updated.getName()).isEqualTo("Updated Name"));
    }
}
