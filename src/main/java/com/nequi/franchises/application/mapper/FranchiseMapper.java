package com.nequi.franchises.application.mapper;

import com.nequi.franchises.application.dto.CreateFranchiseRequest;
import com.nequi.franchises.application.dto.FranchiseDto;
import com.nequi.franchises.domain.model.Franchise;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FranchiseMapper {

    private final BranchMapper branchMapper;

    public Franchise toEntity(CreateFranchiseRequest request) {
        return Franchise.builder()
                .name(request.getName())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public FranchiseDto toDto(Franchise franchise) {
        return FranchiseDto.builder()
                .id(franchise.getId())
                .name(franchise.getName())
                .createdAt(franchise.getCreatedAt())
                .updatedAt(franchise.getUpdatedAt())
                .branches(franchise.getBranches() != null 
                    ? franchise.getBranches().stream()
                        .map(branchMapper::toDto)
                        .collect(Collectors.toList())
                    : null)
                .build();
    }

    public FranchiseDto toDtoWithoutBranches(Franchise franchise) {
        return FranchiseDto.builder()
                .id(franchise.getId())
                .name(franchise.getName())
                .createdAt(franchise.getCreatedAt())
                .updatedAt(franchise.getUpdatedAt())
                .build();
    }

    public Flux<FranchiseDto> toDtoFlux(Flux<Franchise> franchises) {
        return franchises.map(this::toDtoWithoutBranches);
    }
}
