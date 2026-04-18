package com.nequi.franchises.application.service;

import com.nequi.franchises.application.dto.BranchDto;
import com.nequi.franchises.application.dto.CreateBranchRequest;
import com.nequi.franchises.application.dto.UpdateNameDto;
import com.nequi.franchises.application.mapper.BranchMapper;
import com.nequi.franchises.domain.exception.BranchNotFoundException;
import com.nequi.franchises.domain.exception.FranchiseNotFoundException;
import com.nequi.franchises.domain.exception.ValidationException;
import com.nequi.franchises.domain.model.Branch;
import com.nequi.franchises.domain.repository.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing branches within franchises.
 * 
 * DESIGN: In MongoDB, branches are embedded in Franchise documents.
 * This service provides operations to manage branches by:
 * - Loading the parent franchise
 * - Modifying the embedded branches array
 * - Saving the updated franchise document
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    private final FranchiseRepository franchiseRepository;
    private final BranchMapper branchMapper;
    private final TransactionalOperator transactionalOperator;

    /**
     * Creates a new branch within a franchise.
     * Uses reactive transaction for atomicity.
     * 
     * PATTERN: TransactionalOperator.execute() for reactive transactions
     * - Loads franchise (read)
     * - Modifies embedded branches array (write)
     * - Saves franchise (write)
     * All operations are atomic - rollback on failure.
     */
    public Mono<BranchDto> createBranch(CreateBranchRequest request) {
        if (request.getFranchiseId() == null || request.getFranchiseId().isBlank()) {
            return Mono.error(new ValidationException("Franchise ID is required"));
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return Mono.error(new ValidationException("Branch name cannot be empty"));
        }
        
        return Mono.defer(() -> 
            franchiseRepository.findById(request.getFranchiseId())
                    .switchIfEmpty(Mono.error(new FranchiseNotFoundException(request.getFranchiseId())))
                    .flatMap(franchise -> {
                        // Check for duplicate branch name
                        if (franchise.getBranches() != null && 
                            franchise.getBranches().stream().anyMatch(b -> b.getName().equalsIgnoreCase(request.getName()))) {
                            return Mono.error(new ValidationException("Branch name already exists in this franchise"));
                        }
                        
                        Branch newBranch = Branch.builder()
                                .id(UUID.randomUUID().toString())
                                .name(request.getName())
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                        
                        franchise.addBranch(newBranch);
                        
                        log.info("Creating branch in franchise: {}", request.getFranchiseId());
                        return franchiseRepository.save(franchise)
                                .map(saved -> branchMapper.toDto(newBranch));
                    })
                    .as(transactionalOperator::transactional)
        );
    }

    /**
     * Gets a branch by ID within a franchise.
     * Returns the branch if found in the franchise's embedded array.
     */
    public Mono<BranchDto> getBranchById(String franchiseId, String branchId) {
        if (franchiseId == null || franchiseId.isBlank() || branchId == null || branchId.isBlank()) {
            return Mono.error(new ValidationException("Franchise ID and Branch ID are required"));
        }
        
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new FranchiseNotFoundException(franchiseId)))
                .flatMap(franchise -> {
                    if (franchise.getBranches() == null) {
                        return Mono.error(new BranchNotFoundException(branchId));
                    }
                    return franchise.getBranches().stream()
                            .filter(b -> branchId.equals(b.getId()))
                            .findFirst()
                            .map(branchMapper::toDto)
                            .map(Mono::just)
                            .orElseGet(() -> Mono.error(new BranchNotFoundException(branchId)));
                });
    }

    /**
     * Gets all branches for a franchise.
     */
    public Flux<BranchDto> getBranchesByFranchiseId(String franchiseId) {
        if (franchiseId == null || franchiseId.isBlank()) {
            return Flux.error(new ValidationException("Franchise ID is required"));
        }
        
        return franchiseRepository.findById(franchiseId)
                .switchIfEmpty(Mono.error(new FranchiseNotFoundException(franchiseId)))
                .flatMapMany(franchise -> {
                    if (franchise.getBranches() == null) {
                        return Flux.empty();
                    }
                    return Flux.fromIterable(franchise.getBranches())
                            .map(branchMapper::toDto);
                });
    }

    /**
     * Updates a branch name.
     * Modifies the embedded branch within the franchise document.
     */
    public Mono<BranchDto> updateBranchName(String franchiseId, String branchId, UpdateNameDto request) {
        if (franchiseId == null || franchiseId.isBlank() || branchId == null || branchId.isBlank()) {
            return Mono.error(new ValidationException("Franchise ID and Branch ID are required"));
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return Mono.error(new ValidationException("Branch name cannot be empty"));
        }
        
        return Mono.defer(() -> 
            franchiseRepository.findById(franchiseId)
                    .switchIfEmpty(Mono.error(new FranchiseNotFoundException(franchiseId)))
                    .flatMap(franchise -> {
                        if (franchise.getBranches() == null) {
                            return Mono.error(new BranchNotFoundException(branchId));
                        }
                        
                        Branch branch = franchise.getBranches().stream()
                                .filter(b -> branchId.equals(b.getId()))
                                .findFirst()
                                .orElse(null);
                        
                        if (branch == null) {
                            return Mono.error(new BranchNotFoundException(branchId));
                        }
                        
                        branch.updateName(request.getName());
                        
                        return franchiseRepository.save(franchise)
                                .map(saved -> branchMapper.toDto(branch));
                    })
        ).as(transactionalOperator::transactional);
    }

    /**
     * Deletes a branch from a franchise.
     * Removes the branch from the embedded array.
     */
    public Mono<Void> deleteBranch(String franchiseId, String branchId) {
        if (franchiseId == null || franchiseId.isBlank() || branchId == null || branchId.isBlank()) {
            return Mono.error(new ValidationException("Franchise ID and Branch ID are required"));
        }
        
        return Mono.defer(() -> 
            franchiseRepository.findById(franchiseId)
                    .switchIfEmpty(Mono.error(new FranchiseNotFoundException(franchiseId)))
                    .flatMap(franchise -> {
                        if (franchise.getBranches() == null) {
                            return Mono.error(new BranchNotFoundException(branchId));
                        }
                        
                        boolean removed = franchise.removeBranch(branchId);
                        if (!removed) {
                            return Mono.error(new BranchNotFoundException(branchId));
                        }
                        return franchiseRepository.save(franchise).then();
                    })
        ).as(transactionalOperator::transactional);
    }

    /**
     * Validates that a branch exists within a franchise.
     * Returns true if the branch exists, false otherwise.
     */
    public Mono<Boolean> branchExistsInFranchise(String franchiseId, String branchId) {
        if (franchiseId == null || franchiseId.isBlank() || branchId == null || branchId.isBlank()) {
            return Mono.just(false);
        }
        
        return franchiseRepository.findById(franchiseId)
                .map(franchise -> {
                    if (franchise.getBranches() == null) {
                        return false;
                    }
                    return franchise.getBranches().stream()
                            .anyMatch(b -> branchId.equals(b.getId()));
                })
                .defaultIfEmpty(false);
    }
}
