package com.nequi.franchises.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Product.
 * Uses String IDs for MongoDB compatibility.
 * References branch by ID (not embedded).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String id;
    private String branchId;  // Reference to branch (not embedded)
    private String name;
    private Integer stock;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
