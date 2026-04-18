package com.nequi.franchises.application.service;

import com.nequi.franchises.application.dto.CreateFranchiseRequest;
import com.nequi.franchises.application.dto.FranchiseDto;
import com.nequi.franchises.application.dto.UpdateNameDto;
import com.nequi.franchises.application.mapper.FranchiseMapper;
import com.nequi.franchises.domain.event.FranchiseCreatedEvent;
import com.nequi.franchises.domain.event.FranchiseUpdatedEvent;
import com.nequi.franchises.domain.exception.FranchiseNotFoundException;
import com.nequi.franchises.domain.exception.ValidationException;
import com.nequi.franchises.domain.model.Franchise;
import com.nequi.franchises.domain.publisher.DomainEventPublisher;
import com.nequi.franchises.domain.repository.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FranchiseCommandServiceTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private FranchiseMapper franchiseMapper;

    @Mock
    private DomainEventPublisher eventPublisher;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private FranchiseCommandService franchiseCommandService;

    private Franchise sampleFranchise;
    private FranchiseDto sampleFranchiseDto;

    @BeforeEach
    void setUp() {
        sampleFranchise = Franchise.builder()
                .id("507f1f77bcf86cd799439011")
                .name("Test Franchise")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .branches(new ArrayList<>())
                .build();

        sampleFranchiseDto = FranchiseDto.builder()
                .id("507f1f77bcf86cd799439011")
                .name("Test Franchise")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Mock TransactionalOperator to pass-through (suppress type safety for mocks)
        lenient().doAnswer(invocation -> invocation.getArgument(0))
                .when(transactionalOperator).transactional(any(Mono.class));
        lenient().doAnswer(invocation -> invocation.getArgument(0))
                .when(transactionalOperator).transactional(any(Flux.class));
    }

    @Test
    @DisplayName("createFranchise - should create franchise successfully")
    void createFranchise_Success() {
        // Given
        CreateFranchiseRequest request = new CreateFranchiseRequest();
        request.setName("Test Franchise");

        when(franchiseMapper.toEntity(request)).thenReturn(sampleFranchise);
        when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.just(sampleFranchise));
        when(franchiseMapper.toDtoWithoutBranches(sampleFranchise)).thenReturn(sampleFranchiseDto);
        doNothing().when(eventPublisher).publish(any(FranchiseCreatedEvent.class));

        // When & Then
        StepVerifier.create(franchiseCommandService.createFranchise(request))
                .expectNextMatches(dto -> dto.getName().equals("Test Franchise"))
                .verifyComplete();

        verify(eventPublisher).publish(argThat(event -> 
            event instanceof FranchiseCreatedEvent && 
            ((FranchiseCreatedEvent) event).getFranchiseName().equals("Test Franchise")
        ));
    }

    @Test
    @DisplayName("createFranchise - should publish event after creation")
    void createFranchise_PublishesEvent() {
        // Given
        CreateFranchiseRequest request = new CreateFranchiseRequest();
        request.setName("Event Test Franchise");

        when(franchiseMapper.toEntity(request)).thenReturn(sampleFranchise);
        when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.just(sampleFranchise));
        when(franchiseMapper.toDtoWithoutBranches(sampleFranchise)).thenReturn(sampleFranchiseDto);

        // When
        franchiseCommandService.createFranchise(request).block();

        // Then
        verify(eventPublisher, times(1)).publish(any(FranchiseCreatedEvent.class));
    }

    @Test
    @DisplayName("updateFranchiseName - should update name successfully")
    void updateFranchiseName_Success() {
        // Given
        String id = "507f1f77bcf86cd799439011";
        UpdateNameDto request = new UpdateNameDto();
        request.setName("Updated Name");

        when(franchiseRepository.findById(id)).thenReturn(Mono.just(sampleFranchise));
        when(franchiseRepository.save(any(Franchise.class))).thenReturn(Mono.just(sampleFranchise));
        when(franchiseMapper.toDtoWithoutBranches(sampleFranchise)).thenReturn(sampleFranchiseDto);
        doNothing().when(eventPublisher).publish(any(FranchiseUpdatedEvent.class));

        // When & Then
        StepVerifier.create(franchiseCommandService.updateFranchiseName(id, request))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("updateFranchiseName - should error when id is blank")
    void updateFranchiseName_BlankId_Error() {
        // Given
        UpdateNameDto request = new UpdateNameDto();
        request.setName("Updated Name");

        // When & Then
        StepVerifier.create(franchiseCommandService.updateFranchiseName("", request))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("updateFranchiseName - should error when name is empty")
    void updateFranchiseName_EmptyName_Error() {
        // Given
        String id = "507f1f77bcf86cd799439011";
        UpdateNameDto request = new UpdateNameDto();
        request.setName("   ");

        // When & Then
        StepVerifier.create(franchiseCommandService.updateFranchiseName(id, request))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("updateFranchiseName - should error when franchise not found")
    void updateFranchiseName_NotFound_Error() {
        // Given
        String id = "nonexistent-id";
        UpdateNameDto request = new UpdateNameDto();
        request.setName("Updated Name");

        when(franchiseRepository.findById(id)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(franchiseCommandService.updateFranchiseName(id, request))
                .expectError(FranchiseNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("deleteFranchise - should delete successfully")
    void deleteFranchise_Success() {
        // Given
        String id = "507f1f77bcf86cd799439011";

        when(franchiseRepository.existsById(id)).thenReturn(Mono.just(true));
        when(franchiseRepository.deleteById(id)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(franchiseCommandService.deleteFranchise(id))
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteFranchise - should error when id is blank")
    void deleteFranchise_BlankId_Error() {
        // When & Then
        StepVerifier.create(franchiseCommandService.deleteFranchise(""))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("deleteFranchise - should error when franchise not found")
    void deleteFranchise_NotFound_Error() {
        // Given
        String id = "nonexistent-id";

        when(franchiseRepository.existsById(id)).thenReturn(Mono.just(false));

        // When & Then
        StepVerifier.create(franchiseCommandService.deleteFranchise(id))
                .expectError(FranchiseNotFoundException.class)
                .verify();
    }
}
