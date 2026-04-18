package com.nequi.franchises;

import com.nequi.franchises.infrastructure.config.properties.ApplicationProperties;
import com.nequi.franchises.infrastructure.config.properties.JwtProperties;
import com.nequi.franchises.infrastructure.config.properties.MongoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Franchises API Application - Spring Boot Entry Point.
 *
 * CONFIGURACIÓN:
 *   - @EnableConfigurationProperties: Habilita clases record inmutables
 *   - Validación automática con @Validated
 *   - Fail-fast si faltan propiedades requeridas
 */
@SpringBootApplication
@EnableConfigurationProperties({
    MongoProperties.class,
    JwtProperties.class,
    ApplicationProperties.class
})
public class FranchisesApplication {

    public static void main(String[] args) {
        SpringApplication.run(FranchisesApplication.class, args);
    }
}
