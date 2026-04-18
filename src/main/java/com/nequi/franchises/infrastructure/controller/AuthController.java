package com.nequi.franchises.infrastructure.controller;

import com.nequi.franchises.infrastructure.security.JwtTokenProvider;
import com.nequi.franchises.infrastructure.security.RefreshTokenService;
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
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // Access token expiration: 15 minutes
    private static final long ACCESS_TOKEN_EXPIRATION_MINUTES = 15;

    /**
     * Authenticate user and return token pair (access + refresh).
     * 
     * PRODUCTION: Validate credentials against real user database.
     */
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

    /**
     * Refresh access token using refresh token.
     * Implements token rotation: new refresh token issued, old one invalidated.
     */
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

    /**
     * Logout - revoke refresh token.
     */
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
    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;
        
        @NotBlank(message = "Password is required")
        private String password;
        
        private String role;
    }

    @Data
    public static class RefreshRequest {
        @NotBlank(message = "Refresh token is required")
        private String refreshToken;
    }

    @Data
    @RequiredArgsConstructor
    public static class AuthResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String tokenType;
        private final long expiresIn;
    }
}
