package com.nequi.franchises.contract;

import com.nequi.franchises.application.dto.CreateBranchRequest;
import com.nequi.franchises.application.dto.CreateFranchiseRequest;
import com.nequi.franchises.application.dto.CreateProductRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

/**
 * Contract tests for Franchise API.
 * 
 * Validates:
 * - HTTP Status Codes
 * - JSON Response Structure
 * - Required Fields Presence
 * - API Consistency (Nested vs Global paths)
 * 
 * These tests act as "API Contract Safeguards" ensuring backward compatibility.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class FranchiseApiContractTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final String BASE_URL = "/api/v1";

    // ==================== JSON Path Validators ====================

    private Consumer<org.springframework.test.web.reactive.server.JsonPathAssertions> hasRequiredFranchiseFields() {
        return json -> json.exists();
    }

    private Consumer<org.springframework.test.web.reactive.server.JsonPathAssertions> hasRequiredBranchFields() {
        return json -> json.exists();
    }

    private Consumer<org.springframework.test.web.reactive.server.JsonPathAssertions> hasRequiredProductFields() {
        return json -> json.exists();
    }

    // ==================== WRITE MODEL: Nested API Tests ====================

    @Nested
    @DisplayName("WRITE MODEL - Nested API (Commands)")
    class WriteModelNestedApi {

        @Test
        @DisplayName("POST /franchises - Should create franchise with 201 and valid structure")
        void createFranchiseContract() {
            // Given
            CreateFranchiseRequest request = CreateFranchiseRequest.builder()
                    .name("Contract Test Franchise")
                    .build();

            // Then
            webTestClient.post()
                    .uri(BASE_URL + "/franchises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(request), CreateFranchiseRequest.class)
                    .exchange()
                    .expectStatus().isCreated() // 201
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$.id").isNotEmpty()
                    .jsonPath("$.name").isEqualTo("Contract Test Franchise")
                    .jsonPath("$.createdAt").exists()
                    .jsonPath("$.updatedAt").exists();
        }

        @Test
        @DisplayName("POST /franchises/{fid}/branches - Should create branch within franchise (nested)")
        void createBranchNestedContract() {
            // First create a franchise
            CreateFranchiseRequest franchiseReq = CreateFranchiseRequest.builder()
                    .name("Parent Franchise")
                    .build();

            String franchiseId = webTestClient.post()
                    .uri(BASE_URL + "/franchises")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(franchiseReq), CreateFranchiseRequest.class)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.id").exists()
                    .returnResult()
                    .getResponseBodyContent().toString();

            // Extract ID from JSON (simplified)
            // Then create branch nested
            CreateBranchRequest branchReq = CreateBranchRequest.builder()
                    .name("Nested Branch")
                    .build();

            webTestClient.post()
                    .uri(BASE_URL + "/franchises/{franchiseId}/branches", extractId(franchiseId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Mono.just(branchReq), CreateBranchRequest.class)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.id").exists()
                    .jsonPath("$.name").isEqualTo("Nested Branch");
        }

        @Test
        @DisplayName("POST /franchises/{fid}/branches/{bid}/products - Should create product nested (requires hierarchy)")
        void createProductNestedContract() {
            // This test validates the WRITE MODEL: product creation requires franchise+branch context
            CreateFranchiseRequest franchiseReq = CreateFranchiseRequest.builder()
                    .name("Hierarchy Franchise")
                    .build();

            String franchiseBody = webTestClient.post()
                    .uri(BASE_URL + "/franchises")
                    .bodyValue(franchiseReq)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody().returnResult().getResponseBodyContent().toString();

            String franchiseId = extractId(franchiseBody);

            // Create branch
            CreateBranchRequest branchReq = CreateBranchRequest.builder()
                    .name("Hierarchy Branch")
                    .build();

            String branchBody = webTestClient.post()
                    .uri(BASE_URL + "/franchises/{fid}/branches", franchiseId)
                    .bodyValue(branchReq)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody().returnResult().getResponseBodyContent().toString();

            String branchId = extractId(branchBody);

            // Create product nested - WRITE MODEL
            CreateProductRequest productReq = CreateProductRequest.builder()
                    .name("Nested Product")
                    .stock(100)
                    .build();

            webTestClient.post()
                    .uri(BASE_URL + "/franchises/{fid}/branches/{bid}/products", franchiseId, branchId)
                    .bodyValue(productReq)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.id").exists()
                    .jsonPath("$.branchId").isEqualTo(branchId)
                    .jsonPath("$.name").isEqualTo("Nested Product")
                    .jsonPath("$.stock").isEqualTo(100);
        }

        @Test
        @DisplayName("POST /franchises/{fid}/branches/{bid}/products - Should reject 404 when branch not in franchise")
        void createProductNestedInvalidBranchContract() {
            // WRITE MODEL enforces validation: branch must exist in franchise
            CreateFranchiseRequest franchiseReq = CreateFranchiseRequest.builder()
                    .name("Franchise with no branches")
                    .build();

            String franchiseBody = webTestClient.post()
                    .uri(BASE_URL + "/franchises")
                    .bodyValue(franchiseReq)
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody().returnResult().getResponseBodyContent().toString();

            String franchiseId = extractId(franchiseBody);

            // Try to create product with invalid branch ID
            CreateProductRequest productReq = CreateProductRequest.builder()
                    .name("Invalid Product")
                    .stock(50)
                    .build();

            webTestClient.post()
                    .uri(BASE_URL + "/franchises/{fid}/branches/{bid}/products", franchiseId, "invalid-branch-id")
                    .bodyValue(productReq)
                    .exchange()
                    .expectStatus().isNotFound(); // 404 - Branch not found in franchise
        }
    }

    // ==================== READ MODEL: Global API Tests ====================

    @Nested
    @DisplayName("READ MODEL - Global API (Queries)")
    class ReadModelGlobalApi {

        @Test
        @DisplayName("GET /products/{id} - Should return product by ID (global access)")
        void getProductByIdGlobalContract() {
            // First create a product
            CreateFranchiseRequest franchiseReq = CreateFranchiseRequest.builder()
                    .name("Global Read Franchise")
                    .build();

            String franchiseId = extractId(webTestClient.post()
                    .uri(BASE_URL + "/franchises")
                    .bodyValue(franchiseReq)
                    .exchange()
                    .expectBody().returnResult().getResponseBodyContent().toString());

            CreateBranchRequest branchReq = CreateBranchRequest.builder()
                    .name("Global Read Branch")
                    .build();

            String branchId = extractId(webTestClient.post()
                    .uri(BASE_URL + "/franchises/{fid}/branches", franchiseId)
                    .bodyValue(branchReq)
                    .exchange()
                    .expectBody().returnResult().getResponseBodyContent().toString());

            CreateProductRequest productReq = CreateProductRequest.builder()
                    .name("Global Product")
                    .stock(75)
                    .build();

            String productBody = webTestClient.post()
                    .uri(BASE_URL + "/franchises/{fid}/branches/{bid}/products", franchiseId, branchId)
                    .bodyValue(productReq)
                    .exchange()
                    .expectBody().returnResult().getResponseBodyContent().toString();

            String productId = extractId(productBody);

            // READ MODEL: Access product directly by ID (no hierarchy validation)
            webTestClient.get()
                    .uri(BASE_URL + "/products/{id}", productId)
                    .exchange()
                    .expectStatus().isOk() // 200
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(productId)
                    .jsonPath("$.name").isEqualTo("Global Product")
                    .jsonPath("$.stock").isEqualTo(75)
                    .jsonPath("$.branchId").isEqualTo(branchId);
        }

        @Test
        @DisplayName("GET /products - Should return list structure (READ MODEL)")
        void getAllProductsContract() {
            webTestClient.get()
                    .uri(BASE_URL + "/products")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody()
                    .jsonPath("$").isArray();
        }

        @Test
        @DisplayName("GET /franchises/{fid}/branches/{bid}/products - Contextual query (READ MODEL)")
        void getProductsByBranchContextualContract() {
            CreateFranchiseRequest franchiseReq = CreateFranchiseRequest.builder()
                    .name("Contextual Franchise")
                    .build();

            String franchiseId = extractId(webTestClient.post()
                    .uri(BASE_URL + "/franchises")
                    .bodyValue(franchiseReq)
                    .exchange()
                    .expectBody().returnResult().getResponseBodyContent().toString());

            CreateBranchRequest branchReq = CreateBranchRequest.builder()
                    .name("Contextual Branch")
                    .build();

            String branchId = extractId(webTestClient.post()
                    .uri(BASE_URL + "/franchises/{fid}/branches", franchiseId)
                    .bodyValue(branchReq)
                    .exchange()
                    .expectBody().returnResult().getResponseBodyContent().toString());

            // READ MODEL: Contextual query - products by branch
            webTestClient.get()
                    .uri(BASE_URL + "/franchises/{fid}/branches/{bid}/products", franchiseId, branchId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$").isArray();
        }
    }

    // ==================== Error Contract Tests ====================

    @Nested
    @DisplayName("Error Contracts - API Consistency")
    class ErrorContracts {

        @Test
        @DisplayName("GET non-existent franchise - Should return 404 with error structure")
        void notFoundFranchiseContract() {
            webTestClient.get()
                    .uri(BASE_URL + "/franchises/{id}", "non-existent-id-12345")
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.error").exists()
                    .jsonPath("$.message").exists();
        }

        @Test
        @DisplayName("POST franchise with invalid data - Should return 400 (Bad Request)")
        void badRequestContract() {
            CreateFranchiseRequest invalidRequest = CreateFranchiseRequest.builder()
                    .name("") // Invalid: blank name
                    .build();

            webTestClient.post()
                    .uri(BASE_URL + "/franchises")
                    .bodyValue(invalidRequest)
                    .exchange()
                    .expectStatus().isBadRequest(); // 400
        }

        @Test
        @DisplayName("DELETE non-existent product - Should return 404")
        void deleteNonExistentContract() {
            webTestClient.delete()
                    .uri(BASE_URL + "/products/{id}", "non-existent-product")
                    .exchange()
                    .expectStatus().isNotFound(); // 404
        }

        @Test
        @DisplayName("PATCH product stock - Should return 200 with updated structure")
        void updateStockContract() {
            // Setup
            CreateFranchiseRequest fReq = CreateFranchiseRequest.builder().name("Stock Franchise").build();
            String fid = extractId(webTestClient.post().uri(BASE_URL + "/franchises").bodyValue(fReq)
                    .exchange().expectBody().returnResult().getResponseBodyContent().toString());

            CreateBranchRequest bReq = CreateBranchRequest.builder().name("Stock Branch").build();
            String bid = extractId(webTestClient.post().uri(BASE_URL + "/franchises/{fid}/branches", fid)
                    .bodyValue(bReq).exchange().expectBody().returnResult().getResponseBodyContent().toString());

            CreateProductRequest pReq = CreateProductRequest.builder().name("Stock Product").stock(10).build();
            String pid = extractId(webTestClient.post()
                    .uri(BASE_URL + "/franchises/{fid}/branches/{bid}/products", fid, bid)
                    .bodyValue(pReq).exchange().expectBody().returnResult().getResponseBodyContent().toString());

            // Update stock
            String updateBody = "{\"stock\": 50}";

            webTestClient.patch()
                    .uri(BASE_URL + "/products/{id}/stock", pid)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateBody)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(pid)
                    .jsonPath("$.stock").isEqualTo(50);
        }
    }

    // ==================== CQRS Separation Test ====================

    @Nested
    @DisplayName("CQRS Separation Validation")
    class CqrsSeparation {

        @Test
        @DisplayName("WRITE MODEL enforces context validation, READ MODEL does not")
        void cqrsSeparationContract() {
            // This test demonstrates the CQRS separation:
            // WRITE (nested) requires hierarchy validation
            // READ (global) uses direct access

            CreateFranchiseRequest fReq = CreateFranchiseRequest.builder().name("CQRS Test").build();
            String fid = extractId(webTestClient.post().uri(BASE_URL + "/franchises").bodyValue(fReq)
                    .exchange().expectBody().returnResult().getResponseBodyContent().toString());

            CreateBranchRequest bReq = CreateBranchRequest.builder().name("CQRS Branch").build();
            String bid = extractId(webTestClient.post().uri(BASE_URL + "/franchises/{fid}/branches", fid)
                    .bodyValue(bReq).exchange().expectBody().returnResult().getResponseBodyContent().toString());

            CreateProductRequest pReq = CreateProductRequest.builder().name("CQRS Product").stock(100).build();
            String pid = extractId(webTestClient.post()
                    .uri(BASE_URL + "/franchises/{fid}/branches/{bid}/products", fid, bid)
                    .bodyValue(pReq).exchange().expectBody().returnResult().getResponseBodyContent().toString());

            // WRITE MODEL: Creating product with invalid branch returns 404
            CreateProductRequest invalidReq = CreateProductRequest.builder().name("Invalid").stock(1).build();
            webTestClient.post()
                    .uri(BASE_URL + "/franchises/{fid}/branches/{bid}/products", fid, "invalid-branch")
                    .bodyValue(invalidReq)
                    .exchange()
                    .expectStatus().isNotFound(); // WRITE enforces validation

            // READ MODEL: Access product directly by ID (no hierarchy check)
            webTestClient.get()
                    .uri(BASE_URL + "/products/{id}", pid)
                    .exchange()
                    .expectStatus().isOk() // READ uses direct access
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(pid);
        }
    }

    // ==================== Helper Methods ====================

    private String extractId(String responseBody) {
        // Simple extraction - in real tests use JSON parsing
        if (responseBody == null) return "test-id";
        int idIndex = responseBody.indexOf("id=");
        if (idIndex == -1) {
            // Try JSON format
            int jsonIdIndex = responseBody.indexOf("\"id\":\"");
            if (jsonIdIndex == -1) return "test-id";
            int start = jsonIdIndex + 6;
            int end = responseBody.indexOf("\"", start);
            return responseBody.substring(start, end);
        }
        int start = idIndex + 3;
        int end = responseBody.indexOf(",", start);
        if (end == -1) end = responseBody.indexOf("}", start);
        return responseBody.substring(start, end).trim();
    }
}
