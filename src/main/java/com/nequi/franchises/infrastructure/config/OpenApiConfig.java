package com.nequi.franchises.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Configuration for Franchises API.
 * 
 * CONFIGURACIÓN DE DOCUMENTACIÓN INTERACTIVA:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - API Docs: http://localhost:8080/v3/api-docs
 * 
 * USUARIO DE PRUEBA INTEGRADO:
 * - Username: test
 * - Password: test123
 * 
 * Para probar endpoints protegidos en Swagger UI:
 * 1. Ir a /api/v1/auth/login y hacer login con credenciales de prueba
 * 2. Copiar el "accessToken" de la respuesta
 * 3. Click en "Authorize" (botón verde arriba a la derecha)
 * 4. Pegar el token con prefijo "Bearer " (ej: "Bearer eyJhbG...")
 * 5. Click "Authorize" para aplicar
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Franchises API",
                version = "1.0.0",
                description = """
                        API REST reactiva para gestión de franquicias, sucursales y productos.
                        
                        ## Autenticación
                        Esta API usa JWT (JSON Web Tokens) para autenticación stateless.
                        
                        ## Usuario de Prueba
                        Para probar la API, usar estas credenciales:
                        - **Username**: `test`
                        - **Password**: `test123`
                        
                        ## Pasos para probar en Swagger:
                        1. Ir al endpoint **POST /api/v1/auth/login**
                        2. Usar el body: `{"username":"test","password":"test123"}`
                        3. Copiar el `accessToken` de la respuesta
                        4. Click en **Authorize** (🔓 botón verde arriba)
                        5. Pegar: `Bearer TU_TOKEN_AQUI`
                        6. Click **Authorize** → ahora todos los endpoints funcionan
                        
                        ## Características
                        - ✅ Arquitectura Limpia (Clean Architecture)
                        - ✅ Programación Reactiva con WebFlux
                        - ✅ MongoDB Atlas para persistencia
                        - ✅ JWT Authentication con Refresh Tokens
                        - ✅ Circuit Breaker y Rate Limiting
                        - ✅ Desplegado en AWS ECS Fargate
                        """,
                contact = @Contact(
                        name = "Franchises API Team",
                        email = "dev@franchises.com"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        // Servers auto-detected from incoming request by springdoc.
        // When accessed from AWS ALB, Swagger uses the ALB URL.
        // When accessed locally, Swagger uses localhost.
        security = @SecurityRequirement(name = "bearerAuth"),
        tags = {
                @Tag(name = "Authentication", description = "Login, logout y refresh tokens"),
                @Tag(name = "Franchises", description = "Gestión de franquicias"),
                @Tag(name = "Branches", description = "Gestión de sucursales"),
                @Tag(name = "Products", description = "Gestión de productos")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Token - Obtener via /api/v1/auth/login",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
    // Configuration via annotations only
}
