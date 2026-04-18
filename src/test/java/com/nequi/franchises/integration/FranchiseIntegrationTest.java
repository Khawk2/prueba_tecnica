package com.nequi.franchises.integration;

import com.nequi.franchises.application.dto.CreateFranchiseRequest;
import com.nequi.franchises.application.dto.FranchiseDto;
import com.nequi.franchises.domain.model.Franchise;
import com.nequi.franchises.domain.repository.FranchiseRepository;
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
import reactor.test.StepVerifier;

import java.time.Duration;

/**
 * Integration tests using Testcontainers with MongoDB.
 * Tests full request flow from controller to database.
 */
@SpringBootTest
@AutoConfigureWebTestClient
@Testcontainers
@Tag("integration")
class FranchiseIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
            .withStartupTimeout(Duration.ofMinutes(2));

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private FranchiseRepository franchiseRepository;

    @BeforeEach
    void setUp() {
        // Clean database before each test
        franchiseRepository.deleteAll().block();
    }

    @Test
    @DisplayName("POST /api/v1/franchises - creates franchise")
    void createFranchise_Success() {
        // Given
        CreateFranchiseRequest request = new CreateFranchiseRequest();
        request.setName("Integration Test Franchise");

        // When & Then
        webTestClient.post()
                .uri("/api/v1/franchises")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(FranchiseDto.class)
                .value(dto -> {
                    assert dto.getName().equals("Integration Test Franchise");
                    assert dto.getId() != null;
                });
    }

    @Test
    @DisplayName("GET /api/v1/franchises/{id} - returns franchise")
    void getFranchise_Success() {
        // Given
        Franchise franchise = Franchise.builder()
                .name("Test Franchise")
                .build();
        Franchise saved = franchiseRepository.save(franchise).block();

        // When & Then
        webTestClient.get()
                .uri("/api/v1/franchises/{id}", saved.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(FranchiseDto.class)
                .value(dto -> {
                    assert dto.getId().equals(saved.getId());
                    assert dto.getName().equals("Test Franchise");
                });
    }

    @Test
    @DisplayName("GET /api/v1/franchises/{id} - returns 404 for non-existent")
    void getFranchise_NotFound() {
        // When & Then
        webTestClient.get()
                .uri("/api/v1/franchises/{id}", "nonexistent-id")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("DELETE /api/v1/franchises/{id} - removes franchise")
    void deleteFranchise_Success() {
        // Given
        Franchise franchise = Franchise.builder()
                .name("To Delete")
                .build();
        Franchise saved = franchiseRepository.save(franchise).block();

        // When & Then
        webTestClient.delete()
                .uri("/api/v1/franchises/{id}", saved.getId())
                .exchange()
                .expectStatus().isNoContent();

        // Verify deletion
        StepVerifier.create(franchiseRepository.existsById(saved.getId()))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    @DisplayName("GET /api/v1/franchises - returns paginated list")
    void getAllFranchises_Paginated() {
        // Given - Create multiple franchises
        franchiseRepository.save(Franchise.builder().name("Franchise 1").build()).block();
        franchiseRepository.save(Franchise.builder().name("Franchise 2").build()).block();
        franchiseRepository.save(Franchise.builder().name("Franchise 3").build()).block();

        // When & Then
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/franchises")
                        .queryParam("page", 0)
                        .queryParam("size", 2)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(FranchiseDto.class)
                .hasSize(2);
    }
}
