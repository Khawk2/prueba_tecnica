package com.nequi.franchises.application.service;

import com.nequi.franchises.application.dto.FranchiseDto;
import com.nequi.franchises.application.mapper.FranchiseMapper;
import com.nequi.franchises.domain.exception.FranchiseNotFoundException;
import com.nequi.franchises.domain.exception.ValidationException;
import com.nequi.franchises.domain.repository.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service responsible for franchise read operations (queries).
 * Follows CQRS pattern - separates read operations from writes.
 * Only depends on FranchiseRepository (single responsibility).
 * 
 * NOTE: Read-only operations don't require explicit transactions in MongoDB reactive.
 */
@Service
@RequiredArgsConstructor
public class FranchiseQueryService {

    private final FranchiseRepository franchiseRepository;
    private final FranchiseMapper franchiseMapper;

    /**
     * Gets a franchise by ID with all its branches.
     */
    public Mono<FranchiseDto> getFranchiseById(String id) {
        if (id == null || id.isBlank()) {
            return Mono.error(new ValidationException("Franchise ID is required"));
        }
        return franchiseRepository.findByIdWithBranches(id)
                .switchIfEmpty(Mono.error(new FranchiseNotFoundException(id)))
                .map(franchiseMapper::toDto);
    }

    /**
     * Gets all franchises with pagination.
     */
    public Flux<FranchiseDto> getAllFranchises(Pageable pageable) {
        return franchiseRepository.findAllBy(pageable)
                .map(franchiseMapper::toDtoWithoutBranches);
    }

    /**
     * Checks if a franchise exists by ID.
     */
    public Mono<Boolean> franchiseExists(String id) {
        if (id == null || id.isBlank()) {
            return Mono.just(false);
        }
        return franchiseRepository.existsById(id);
    }
}
