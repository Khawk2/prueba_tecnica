package com.nequi.franchises.infrastructure.controller;

import com.nequi.franchises.application.dto.BranchDto;
import com.nequi.franchises.application.dto.CreateBranchRequest;
import com.nequi.franchises.application.dto.UpdateNameDto;
import com.nequi.franchises.application.service.BranchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller for branch operations.
 * 
 * DESIGN: Branches are embedded within Franchise documents in MongoDB.
 * All endpoints are nested under franchises to reflect this relationship.
 */
@RestController
@RequestMapping("/api/v1/franchises/{franchiseId}/branches")
@RequiredArgsConstructor
@Validated
@Tag(name = "Branches", description = "Gestión de sucursales - Crear y consultar sucursales de una franquicia")
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    public Mono<ResponseEntity<BranchDto>> createBranch(
            @PathVariable String franchiseId,
            @Valid @RequestBody CreateBranchRequest request) {
        // Set franchiseId from path
        request.setFranchiseId(franchiseId);
        return branchService.createBranch(request)
                .map(branch -> ResponseEntity.status(HttpStatus.CREATED).body(branch));
    }

    @GetMapping("/{branchId}")
    public Mono<ResponseEntity<BranchDto>> getBranchById(
            @PathVariable String franchiseId,
            @PathVariable String branchId) {
        return branchService.getBranchById(franchiseId, branchId)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<BranchDto> getBranchesByFranchiseId(@PathVariable String franchiseId) {
        return branchService.getBranchesByFranchiseId(franchiseId);
    }

    @DeleteMapping("/{branchId}")
    public Mono<ResponseEntity<Void>> deleteBranch(
            @PathVariable String franchiseId,
            @PathVariable String branchId) {
        return branchService.deleteBranch(franchiseId, branchId)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()));
    }

    @PatchMapping("/{branchId}/name")
    public Mono<ResponseEntity<BranchDto>> updateBranchName(
            @PathVariable String franchiseId,
            @PathVariable String branchId,
            @Valid @RequestBody UpdateNameDto request) {
        return branchService.updateBranchName(franchiseId, branchId, request)
                .map(ResponseEntity::ok);
    }
}
