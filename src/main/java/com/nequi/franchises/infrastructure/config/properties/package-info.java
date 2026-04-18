/**
 * Configuration Properties - Immutable Value Objects
 * 
 * Este paquete contiene clases record inmutables para configuración.
 * 
 * PRINCIPIOS:
 * - Inmutabilidad: Configuración no cambia en runtime
 * - Validación: @Validated con @NotBlank, @Min, etc.
 * - Fail-fast: La app no inicia si la configuración es inválida
 * 
 * USO:
 *   @EnableConfigurationProperties({MongoProperties.class, JwtProperties.class})
 */
package com.nequi.franchises.infrastructure.config.properties;
