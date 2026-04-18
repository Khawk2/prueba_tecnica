package com.nequi.franchises.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for top product report.
 * Uses String IDs for MongoDB ObjectId compatibility.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDto {
    private String productId;
    private String productName;
    private Integer stock;
    private String branchId;
    private String branchName;
}
