package com.nequi.franchises.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Branch (embedded in Franchise).
 * Uses String IDs for MongoDB compatibility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDto {
    private String id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Note: No franchiseId - branches are embedded in franchise context
}
