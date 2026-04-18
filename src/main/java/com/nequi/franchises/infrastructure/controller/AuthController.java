package com.nequi.franchises.infrastructure.controller;

import com.nequi.franchises.infrastructure.security.JwtTokenProvider;
import com.nequi.franchises.infrastructure.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Authentication controller with refresh token support.
 * 
 * SECURITY FEATURES:
 * - Access tokens: 15 minutes expiration
 * - Refresh tokens: 7 days expiration, stored in MongoDB
 * - Token rotation: new refresh token issued on each refresh
 * - One-time use: refresh tokens invalidated after use
 * - Token revocation: logout revokes refresh token
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, logout y refresh tokens JWT")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // Access token expiration: 15 minutes
    private static final long ACCESS_TOKEN_EXPIRATION_MINUTES = 15;

    /**
     * Authenticate user and return token pair (access + refresh).
     * 
     * USUARIO DE PRUEBA: username="test", password="test123"
     * 
     * PRODUCTION: Validate credentials against real user database.
     */
    @Operation(
            summary = "Login de usuario",
            description = """
                    Autentica al usuario y devuelve un par de tokens (access + refresh).
                    
                    **Usuario de Prueba:**
                    - Username: `test`
                    - Password: `test123`
                    
                    **Response:**
                    - `accessToken`: Token JWT válido por 15 minutos
                    - `refreshToken`: Token válido por 7 días para obtener nuevos access tokens
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login exitoso", 
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos")
            }
    )
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        // PRODUCTION: Validate credentials against database here
        
        String userId = "user-" + System.currentTimeMillis();
        
        List<String> roles = request.getRole() != null 
                ? List.of(request.getRole().toUpperCase())
                : List.of("USER");
        
        // Generate short-lived access token (15 minutes)
        String accessToken = jwtTokenProvider.generateTokenWithExpiration(
                userId, 
                request.getUsername(),
                roles,
                ACCESS_TOKEN_EXPIRATION_MINUTES
        );

        // Create long-lived refresh token (7 days)
        return refreshTokenService.createRefreshToken(userId, request.getUsername())
                .map(refreshToken -> ResponseEntity.ok(
                        new AuthResponse(
                                accessToken,
                                refreshToken.getToken(),
                                "Bearer",
                                ACCESS_TOKEN_EXPIRATION_MINUTES * 60
                        )
                ));
    }

    @Operation(
            summary = "Refrescar access token",
            description = "Usa un refresh token válido para obtener un nuevo par de tokens (rotación de tokens)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nuevo token generado"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado")
    })
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refreshToken(
            @RequestBody RefreshRequest request) {
        
        return refreshTokenService.refreshAccessToken(request.getRefreshToken())
                .map(tokenPair -> ResponseEntity.ok(
                        new AuthResponse(
                                tokenPair.accessToken(),
                                tokenPair.refreshToken(),
                                "Bearer",
                                ACCESS_TOKEN_EXPIRATION_MINUTES * 60
                        )
                ));
    }

    @Operation(
            summary = "Logout de usuario",
            description = "Revoca el refresh token (logout seguro)"
    )
    @ApiResponse(responseCode = "200", description = "Logout exitoso")
    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshToken) {
        
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.just(ResponseEntity.ok().build());
        }
        
        return refreshTokenService.revokeToken(refreshToken)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @Data
    @Schema(description = "Request para login de usuario")
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        @Schema(description = "Nombre de usuario", example = "test", requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;
        
        @NotBlank(message = "Password is required")
        @Schema(description = "Contraseña", example = "test123", requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;
        
        @Schema(description = "Rol opcional (USER, ADMIN)", example = "USER")
        private String role;
    }

    @Data
    @Schema(description = "Request para refrescar token")
    public static class RefreshRequest {
        @NotBlank(message = "Refresh token is required")
        @Schema(description = "Refresh token obtenido del login", requiredMode = Schema.RequiredMode.REQUIRED)
        private String refreshToken;
    }

    @Data
    @RequiredArgsConstructor
    @Schema(description = "Response de autenticación con tokens JWT")
    public static class AuthResponse {
        @Schema(description = "Access token JWT (válido 15 minutos)", example = "eyJhbGciOiJIUzI1NiIs...")
        private final String accessToken;
        
        @Schema(description = "Refresh token (válido 7 días)", example = "550e8400-e29b-41d4-a716-446655440000")
        private final String refreshToken;
        
        @Schema(description = "Tipo de token", example = "Bearer")
        private final String tokenType;
        
        @Schema(description = "Segundos hasta expiración del access token", example = "900")
        private final long expiresIn;
    }
}
