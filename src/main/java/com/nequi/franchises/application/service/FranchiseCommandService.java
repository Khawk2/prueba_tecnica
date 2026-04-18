package com.nequi.franchises.application.service;

import com.nequi.franchises.application.dto.CreateFranchiseRequest;
import com.nequi.franchises.application.dto.FranchiseDto;
import com.nequi.franchises.application.dto.UpdateNameDto;
import com.nequi.franchises.application.mapper.FranchiseMapper;
import com.nequi.franchises.domain.event.FranchiseCreatedEvent;
import com.nequi.franchises.domain.event.FranchiseUpdatedEvent;
import com.nequi.franchises.domain.exception.FranchiseNotFoundException;
import com.nequi.franchises.domain.exception.ValidationException;
import com.nequi.franchises.domain.publisher.DomainEventPublisher;
import com.nequi.franchises.domain.repository.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * Service responsible for franchise write operations (commands).
 * Follows CQRS pattern - separates write operations from reads.
 * Only depends on FranchiseRepository (single responsibility).
 */
@Service
@RequiredArgsConstructor
public class FranchiseCommandService {

    private final FranchiseRepository franchiseRepository;
    private final FranchiseMapper franchiseMapper;
    private final DomainEventPublisher eventPublisher;
    private final TransactionalOperator transactionalOperator;

    /**
     * Creates a new franchise.
     * Publishes FranchiseCreatedEvent after successful creation.
     * Uses reactive transaction for atomicity.
     */
    public Mono<FranchiseDto> createFranchise(CreateFranchiseRequest request) {
        return Mono.defer(() -> 
            Mono.just(request)
                    .map(franchiseMapper::toEntity)
                    .flatMap(franchiseRepository::save)
                    .doOnSuccess(saved -> 
                        eventPublisher.publish(new FranchiseCreatedEvent(saved.getId(), saved.getName()))
                    )
                    .map(franchiseMapper::toDtoWithoutBranches)
        ).as(transactionalOperator::transactional);
    }

    /**
     * Updates franchise name.
     * Publishes FranchiseUpdatedEvent after successful update.
     * Uses find-modify-save pattern for proper domain-driven update.
     * Uses reactive transaction for atomicity.
     */
    public Mono<FranchiseDto> updateFranchiseName(String id, UpdateNameDto request) {
        if (id == null || id.isBlank()) {
            return Mono.error(new ValidationException("Franchise ID is required"));
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return Mono.error(new ValidationException("Franchise name cannot be empty"));
        }
        
        return Mono.defer(() -> 
            franchiseRepository.findById(id)
                    .switchIfEmpty(Mono.error(new FranchiseNotFoundException(id)))
                    .flatMap(franchise -> {
                        franchise.updateName(request.getName());
                        return franchiseRepository.save(franchise);
                    })
                    .doOnSuccess(updated -> 
                        eventPublisher.publish(new FranchiseUpdatedEvent(id, request.getName()))
                    )
                    .map(franchiseMapper::toDtoWithoutBranches)
        ).as(transactionalOperator::transactional);
    }

    /**
     * Deletes a franchise by ID.
     * Cascade deletion is handled at database level.
     * Uses reactive transaction for atomicity.
     */
    public Mono<Void> deleteFranchise(String id) {
        if (id == null || id.isBlank()) {
            return Mono.error(new ValidationException("Franchise ID is required"));
        }
        return Mono.defer(() -> 
            franchiseRepository.existsById(id)
                    .flatMap(exists -> {
                        if (!exists) {
                            return Mono.error(new FranchiseNotFoundException(id));
                        }
                        return franchiseRepository.deleteById(id);
                    })
        ).as(transactionalOperator::transactional);
    }
}
