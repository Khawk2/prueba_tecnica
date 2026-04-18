package com.nequi.franchises.webflux;

import com.nequi.franchises.application.dto.CreateFranchiseRequest;
import com.nequi.franchises.application.dto.FranchiseDto;
import com.nequi.franchises.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;

/**
 * WebFlux controller tests with security.
 * Tests full HTTP request/response cycle.
 */
@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
@Tag("integration")
class FranchiseControllerWebFluxTest {

    @Container
    @SuppressWarnings("resource") // Testcontainers handles lifecycle automatically
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String authToken;

    @BeforeEach
    void setUp() {
        // Generate valid JWT token for tests
        authToken = jwtTokenProvider.generateToken(
                "test-user-123",
                "testuser",
                List.of("USER")
        );
    }

    @Test
    @DisplayName("POST /api/v1/franchises - without auth should return 401")
    void createFranchise_WithoutAuth_Unauthorized() {
        CreateFranchiseRequest request = new CreateFranchiseRequest();
        request.setName("Test Franchise");

        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/franchises - with valid auth should create")
    void createFranchise_WithAuth_Success() {
        CreateFranchiseRequest request = new CreateFranchiseRequest();
        request.setName("Test Franchise");

        webTestClient.post()
                .uri("/api/v1/franchises")
                .header("Authorization", "Bearer " + authToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseDto.class)
                .value(dto -> {
                    assert dto.getName().equals("Test Franchise");
                    assert dto.getId() != null;
                });
    }

    @Test
    @DisplayName("POST /api/v1/franchises - with invalid token should return 401")
    void createFranchise_InvalidToken_Unauthorized() {
        CreateFranchiseRequest request = new CreateFranchiseRequest();
        request.setName("Test Franchise");

        webTestClient.post()
                .uri("/api/v1/franchises")
                .header("Authorization", "Bearer invalid.token.here")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/franchises - with Idempotency-Key should be idempotent")
    void createFranchise_Idempotent() {
        String idempotencyKey = "idem-key-" + System.currentTimeMillis();
        CreateFranchiseRequest request = new CreateFranchiseRequest();
        request.setName("Idempotent Franchise");

        // First request - should succeed
        webTestClient.post()
                .uri("/api/v1/franchises")
                .header("Authorization", "Bearer " + authToken)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated();

        // Second request with same key - should return same result
        webTestClient.post()
                .uri("/api/v1/franchises")
                .header("Authorization", "Bearer " + authToken)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated(); // Or 200 depending on implementation
    }

    @Test
    @DisplayName("GET /api/v1/franchises - with pagination should return page")
    void getAllFranchises_Paginated() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/franchises")
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build())
                .header("Authorization", "Bearer " + authToken)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FranchiseDto.class);
    }

    @Test
    @DisplayName("GET /api/v1/franchises/{id} - not found should return 404")
    void getFranchise_NotFound() {
        webTestClient.get()
                .uri("/api/v1/franchises/{id}", "nonexistent-id-12345")
                .header("Authorization", "Bearer " + authToken)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - should return token pair")
    void login_Success() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new LoginRequest("testuser", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").exists()
                .jsonPath("$.refreshToken").exists()
                .jsonPath("$.tokenType").isEqualTo("Bearer");
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - rate limiting should apply")
    void login_RateLimit() {
        // Make 6 rapid requests (limit is 5 per minute for auth)
        for (int i = 0; i < 6; i++) {
            var spec = webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new LoginRequest("user" + i, "password"));

            if (i < 5) {
                spec.exchange().expectStatus().isOk();
            } else {
                spec.exchange().expectStatus().isEqualTo(429); // Too Many Requests
            }
        }
    }

    record LoginRequest(String username, String password) {}
}
