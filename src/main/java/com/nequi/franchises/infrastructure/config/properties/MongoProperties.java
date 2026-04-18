package com.nequi.franchises.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * MongoDB Configuration Properties - IMMUTABLE
 * ============================================================================
 * Propiedades inmutables para configuración de MongoDB.
 * 
 * PRINCIPIOS:
 * - Inmutabilidad: Record con valores finales
 * - Validación: @NotBlank para fail-fast
 * - Single Source: Solo MONGODB_URI, sin configuración fragmentada
 * 
 * CONFIGURACIÓN:
 *   spring.data.mongodb.uri=${MONGODB_URI}
 */
@Validated
@ConfigurationProperties(prefix = "spring.data.mongodb")
public record MongoProperties(
    
    /**
     * MongoDB Connection URI - Única fuente de configuración.
     * 
     * Formatos:
     *   - Atlas: mongodb+srv://user:pass@cluster.mongodb.net/db?retryWrites=true&w=majority
     *   - Local: mongodb://localhost:27017/database
     */
    @NotBlank(message = "MONGODB_URI is required")
    String uri,
    
    /**
     * Connection pool configuration (optional, sensible defaults).
     */
    ConnectionPoolProperties connectionPool
) {
    
    public MongoProperties {
        // Pool defaults si no se especifica
        if (connectionPool == null) {
            connectionPool = new ConnectionPoolProperties(2, 10, 2000, 5000);
        }
    }
    
    /**
     * Connection pool sub-properties.
     */
    public record ConnectionPoolProperties(
        int minSize,
        int maxSize,
        int maxWaitTime,
        int serverSelectionTimeout
    ) {}
}
