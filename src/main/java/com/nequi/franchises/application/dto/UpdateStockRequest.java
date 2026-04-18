package com.nequi.franchises.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStockRequest {
    
    @NotNull(message = "Stock is required")
    private Integer stock;
}
