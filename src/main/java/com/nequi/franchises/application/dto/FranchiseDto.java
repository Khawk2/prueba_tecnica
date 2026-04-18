package com.nequi.franchises.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object for Franchise.
 * Uses String IDs for MongoDB compatibility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FranchiseDto {
    private String id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BranchDto> branches;
}
