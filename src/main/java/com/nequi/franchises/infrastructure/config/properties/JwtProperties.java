package com.nequi.franchises.infrastructure.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT Configuration Properties - IMMUTABLE
 * ============================================================================
 * Propiedades inmutables para configuración JWT.
 * 
 * Validaciones incluidas (fail-fast):
 * - secret: No puede estar vacío
 * - expiration: Mínimo 60 segundos (1 minuto)
 * - refreshExpiration: Mínimo 3600 segundos (1 hora)
 * 
 * CONFIGURACIÓN:
 *   spring.security.jwt.secret=${JWT_SECRET}
 *   spring.security.jwt.expiration=900000
 *   spring.security.jwt.refresh-expiration=604800000
 */
@Validated
@ConfigurationProperties(prefix = "spring.security.jwt")
public record JwtProperties(
    
    /**
     * JWT Secret - REQUERIDO en producción.
     * Debe ser de al menos 256 bits (32 caracteres) para seguridad.
     */
    @NotBlank(message = "JWT_SECRET is required")
    String secret,
    
    /**
     * JWT Expiration time in milliseconds.
     * Default: 15 minutos (900000 ms)
     * Mínimo: 1 minuto (60000 ms)
     */
    @Min(value = 60000, message = "JWT expiration must be at least 60 seconds")
    long expiration,
    
    /**
     * JWT Refresh Expiration time in milliseconds.
     * Default: 7 días (604800000 ms)
     * Mínimo: 1 hora (3600000 ms)
     */
    @Min(value = 3600000, message = "JWT refresh expiration must be at least 1 hour")
    long refreshExpiration
) {
    
    public JwtProperties {
        // Defaults para desarrollo (pero @NotBlank falla en prod si no se setea)
        if (expiration == 0) expiration = 900000;  // 15 minutos
        if (refreshExpiration == 0) refreshExpiration = 604800000;  // 7 días
    }
    
    /**
     * Verifica si el secret cumple longitud mínima de seguridad.
     * Nota: Esta es una verificación de calidad, no de existencia.
     * La validación @NotBlank ya asegura que existe.
     */
    public boolean isSecure() {
        return secret != null && secret.length() >= 32;
    }
}
