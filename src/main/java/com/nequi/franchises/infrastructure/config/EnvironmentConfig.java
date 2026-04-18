package com.nequi.franchises.infrastructure.config;

import com.nequi.franchises.infrastructure.config.properties.ApplicationProperties;
import com.nequi.franchises.infrastructure.config.properties.MongoProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Enterprise Environment Configuration - SIMPLIFICADO
 * ============================================================================
 * PRINCIPIO: Configuración explícita, fail-fast, separación de responsabilidades.
 *
 * RESPONSABILIDADES:
 *   - Verificar que las propiedades se cargaron correctamente
 *   - Logging estructurado del estado del entorno
 *   - NO validar lógica de negocio (eso va en JwtConfig, SecurityConfig)
 *
 * ARQUITECTURA:
 *   - Usa @ConfigurationProperties inmutables
 *   - Las validaciones @NotBlank, @Min son manejadas por Spring Validation
 *   - NO detección de entorno automática
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class EnvironmentConfig {

    private final ApplicationProperties appProperties;
    private final MongoProperties mongoProperties;

    /**
     * Logging simplificado del entorno al arranque.
     * Las validaciones @NotBlank/@Min causan fail-fast automáticamente.
     */
    @PostConstruct
    public void logEnvironment() {
        boolean isProd = appProperties.isProduction();
        
        // Log simplificado según entorno
        if (isProd) {
            logProduction();
        } else {
            logDevelopment();
        }
    }

    /**
     * Log estructurado para producción.
     */
    private void logProduction() {
        log.info("================================================");
        log.info("Environment: PROD");
        log.info("MongoDB: {} (Secrets Manager)", maskUri(mongoProperties.uri()));
        log.info("Security: ENABLED");
        log.info("Status: READY");
        log.info("================================================");
    }

    /**
     * Log estructurado para desarrollo.
     */
    private void logDevelopment() {
        log.info("================================================");
        log.info("Environment: DEV");
        log.info("Using local development configuration");
        log.info("================================================");
    }

    /**
     * Mascara URI para logging seguro.
     */
    private String maskUri(String uri) {
        if (uri == null) {
            return "not configured";
        }
        return uri.replaceAll("//[^:]+:[^@]+@", "//***@");
    }
}
