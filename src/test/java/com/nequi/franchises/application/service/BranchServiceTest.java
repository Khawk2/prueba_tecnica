package com.nequi.franchises.application.service;

import com.nequi.franchises.application.dto.BranchDto;
import com.nequi.franchises.application.dto.CreateBranchRequest;
import com.nequi.franchises.application.dto.UpdateNameDto;
import com.nequi.franchises.application.mapper.BranchMapper;
import com.nequi.franchises.domain.exception.BranchNotFoundException;
import com.nequi.franchises.domain.exception.FranchiseNotFoundException;
import com.nequi.franchises.domain.exception.ValidationException;
import com.nequi.franchises.domain.model.Branch;
import com.nequi.franchises.domain.model.Franchise;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @Mock
    private BranchMapper branchMapper;

    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private BranchService branchService;

    private Franchise sampleFranchise;
    private Branch sampleBranch;
    private BranchDto sampleBranchDto;

    @BeforeEach
    void setUp() {
        sampleBranch = Branch.builder()
                .id("branch-123")
                .name("Test Branch")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        List<Branch> branches = new ArrayList<>();
        branches.add(sampleBranch);

        sampleFranchise = Franchise.builder()
                .id("franchise-456")
                .name("Test Franchise")
                .branches(branches)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleBranchDto = BranchDto.builder()
                .id("branch-123")
                .name("Test Branch")
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
    @DisplayName("createBranch - should create branch within franchise")
    void createBranch_Success() {
        // Given
        CreateBranchRequest request = new CreateBranchRequest();
        request.setFranchiseId("franchise-456");
        request.setName("New Branch");

        Franchise franchiseWithEmptyBranches = Franchise.builder()
                .id("franchise-456")
                .name("Test Franchise")
                .branches(new ArrayList<>())
                .build();

        when(franchiseRepository.findById("franchise-456")).thenReturn(Mono.just(franchiseWithEmptyBranches));
        when(franchiseRepository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(branchMapper.toDto(any(Branch.class))).thenReturn(BranchDto.builder()
                .name("New Branch")
                .build());

        // When & Then
        StepVerifier.create(branchService.createBranch(request))
                .expectNextMatches(dto -> dto.getName().equals("New Branch"))
                .verifyComplete();
    }

    @Test
    @DisplayName("createBranch - should error when franchise not found")
    void createBranch_FranchiseNotFound_Error() {
        // Given
        CreateBranchRequest request = new CreateBranchRequest();
        request.setFranchiseId("nonexistent");
        request.setName("New Branch");

        when(franchiseRepository.findById("nonexistent")).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(branchService.createBranch(request))
                .expectError(FranchiseNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("createBranch - should error when name is empty")
    void createBranch_EmptyName_Error() {
        // Given
        CreateBranchRequest request = new CreateBranchRequest();
        request.setFranchiseId("franchise-456");
        request.setName("");

        // When & Then
        StepVerifier.create(branchService.createBranch(request))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    @DisplayName("getBranchById - should return branch from franchise")
    void getBranchById_Success() {
        // Given
        String franchiseId = "franchise-456";
        String branchId = "branch-123";

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(sampleFranchise));
        when(branchMapper.toDto(sampleBranch)).thenReturn(sampleBranchDto);

        // When & Then
        StepVerifier.create(branchService.getBranchById(franchiseId, branchId))
                .expectNext(sampleBranchDto)
                .verifyComplete();
    }

    @Test
    @DisplayName("getBranchById - should error when branch not found")
    void getBranchById_NotFound_Error() {
        // Given
        String franchiseId = "franchise-456";
        String branchId = "nonexistent-branch";

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(sampleFranchise));

        // When & Then
        StepVerifier.create(branchService.getBranchById(franchiseId, branchId))
                .expectError(BranchNotFoundException.class)
                .verify();
    }

    @Test
    @DisplayName("getBranchesByFranchiseId - should return all branches")
    void getBranchesByFranchiseId_Success() {
        // Given
        String franchiseId = "franchise-456";

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(sampleFranchise));
        when(branchMapper.toDto(any(Branch.class))).thenReturn(sampleBranchDto);

        // When & Then
        StepVerifier.create(branchService.getBranchesByFranchiseId(franchiseId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    @DisplayName("updateBranchName - should update branch name")
    void updateBranchName_Success() {
        // Given
        String franchiseId = "franchise-456";
        String branchId = "branch-123";
        UpdateNameDto request = new UpdateNameDto();
        request.setName("Updated Branch Name");

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(sampleFranchise));
        when(franchiseRepository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(branchMapper.toDto(any(Branch.class))).thenReturn(BranchDto.builder()
                .id(branchId)
                .name("Updated Branch Name")
                .build());

        // When & Then
        StepVerifier.create(branchService.updateBranchName(franchiseId, branchId, request))
                .expectNextMatches(dto -> dto.getName().equals("Updated Branch Name"))
                .verifyComplete();
    }

    @Test
    @DisplayName("deleteBranch - should remove branch from franchise")
    void deleteBranch_Success() {
        // Given
        String franchiseId = "franchise-456";
        String branchId = "branch-123";

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(sampleFranchise));
        when(franchiseRepository.save(any(Franchise.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When & Then
        StepVerifier.create(branchService.deleteBranch(franchiseId, branchId))
                .verifyComplete();

        // Verify branch was removed
        verify(franchiseRepository).save(argThat(franchise -> 
            franchise.getBranches().stream().noneMatch(b -> b.getId().equals(branchId))
        ));
    }

    @Test
    @DisplayName("branchExistsInFranchise - should return true when branch exists")
    void branchExistsInFranchise_True() {
        // Given
        String franchiseId = "franchise-456";
        String branchId = "branch-123";

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(sampleFranchise));

        // When & Then
        StepVerifier.create(branchService.branchExistsInFranchise(franchiseId, branchId))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    @DisplayName("branchExistsInFranchise - should return false when branch not found")
    void branchExistsInFranchise_False() {
        // Given
        String franchiseId = "franchise-456";
        String branchId = "nonexistent";

        when(franchiseRepository.findById(franchiseId)).thenReturn(Mono.just(sampleFranchise));

        // When & Then
        StepVerifier.create(branchService.branchExistsInFranchise(franchiseId, branchId))
                .expectNext(false)
                .verifyComplete();
    }
}
