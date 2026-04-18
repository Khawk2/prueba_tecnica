package com.nequi.franchises.infrastructure.controller;

import com.nequi.franchises.application.dto.CreateFranchiseRequest;
import com.nequi.franchises.application.dto.FranchiseDto;
import com.nequi.franchises.application.dto.TopProductDto;
import com.nequi.franchises.application.dto.UpdateNameDto;
import com.nequi.franchises.application.service.FranchiseCommandService;
import com.nequi.franchises.application.service.FranchiseQueryService;
import com.nequi.franchises.application.service.TopProductReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller for franchise operations.
 * 
 * REFACTORED: Now follows CQRS pattern by delegating to specialized services:
 * - FranchiseCommandService: Handles write operations (create, update, delete)
 * - FranchiseQueryService: Handles read operations (queries)
 * - TopProductReportService: Handles complex reporting queries
 */
@RestController
@RequestMapping("/api/v1/franchises")
@RequiredArgsConstructor
@Validated
@Tag(name = "Franchises", description = "Gestión de franquicias - Crear, consultar y actualizar franquicias")
public class FranchiseController {

    private final FranchiseCommandService commandService;
    private final FranchiseQueryService queryService;
    private final TopProductReportService reportService;

    // Write operations - Command Service
    @PostMapping
    public Mono<ResponseEntity<FranchiseDto>> createFranchise(@Valid @RequestBody CreateFranchiseRequest request) {
        return commandService.createFranchise(request)
                .map(franchise -> ResponseEntity.status(HttpStatus.CREATED).body(franchise));
    }

    @PatchMapping("/{id}/name")
    public Mono<ResponseEntity<FranchiseDto>> updateFranchiseName(
            @PathVariable String id,
            @Valid @RequestBody UpdateNameDto request) {
        return commandService.updateFranchiseName(id, request)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteFranchise(@PathVariable String id) {
        return commandService.deleteFranchise(id)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    // Read operations - Query Service
    @GetMapping("/{id}")
    public Mono<ResponseEntity<FranchiseDto>> getFranchiseById(@PathVariable String id) {
        return queryService.getFranchiseById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Flux<FranchiseDto> getAllFranchises(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return queryService.getAllFranchises(pageable);
    }

    // Complex reporting - Report Service
    @GetMapping("/{id}/top-products")
    public Flux<TopProductDto> getTopProductsByFranchise(@PathVariable String id) {
        return reportService.getTopProductsByFranchise(id);
    }
}
