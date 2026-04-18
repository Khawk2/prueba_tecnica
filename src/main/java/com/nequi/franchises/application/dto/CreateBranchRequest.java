package com.nequi.franchises.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Branch.
 * Branches are embedded in Franchise documents in MongoDB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBranchRequest {

    @NotNull(message = "Franchise ID is required")
    private String franchiseId;

    @NotBlank(message = "Branch name is required")
    @Size(min = 2, max = 100, message = "Branch name must be between 2 and 100 characters")
    private String name;
}
