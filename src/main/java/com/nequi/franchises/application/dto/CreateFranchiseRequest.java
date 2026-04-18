package com.nequi.franchises.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new Franchise.
 * Validation rules ensure data integrity before processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFranchiseRequest {

    @NotBlank(message = "Franchise name is required")
    @Size(min = 2, max = 100, message = "Franchise name must be between 2 and 100 characters")
    private String name;
}
