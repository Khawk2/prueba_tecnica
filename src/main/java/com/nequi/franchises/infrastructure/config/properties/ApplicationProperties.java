package com.nequi.franchises.infrastructure.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Application Configuration Properties - IMMUTABLE
 * ============================================================================
 * Root configuration properties para la aplicación.
 * 
 * Agrupa todas las propiedades inmutables de la aplicación.
 * Usar @EnableConfigurationProperties en la clase principal.
 */
@ConfigurationProperties(prefix = "app")
public record ApplicationProperties(
    
    /**
     * Active profile - Inyectado automáticamente.
     */
    String profile,
    
    /**
     * Feature flags configuration.
     */
    @NestedConfigurationProperty
    FeatureFlags features,
    
    /**
     * Server configuration.
     */
    @NestedConfigurationProperty
    ServerProperties server
) {
    
    public ApplicationProperties {
        if (features == null) {
            features = new FeatureFlags(false, false, true, true);
        }
        if (server == null) {
            server = new ServerProperties(8080);
        }
    }
    
    /**
     * Feature flags inmutables.
     */
    public record FeatureFlags(
        boolean redis,
        boolean cache,
        boolean metrics,
        Boolean rateLimiting
    ) {
        public FeatureFlags {
            // Default rateLimiting to true if not specified
            if (rateLimiting == null) {
                rateLimiting = true;
            }
        }
    }
    
    /**
     * Server configuration.
     */
    public record ServerProperties(
        int port
    ) {}
    
    /**
     * Determina si es entorno de producción.
     * Nota: Esto es basado en el profile explícito, NO en detección de runtime.
     */
    public boolean isProduction() {
        return "prod".equals(profile);
    }
    
    /**
     * Determina si es entorno de desarrollo.
     */
    public boolean isDevelopment() {
        return "dev".equals(profile) || profile == null || profile.isEmpty();
    }
}
