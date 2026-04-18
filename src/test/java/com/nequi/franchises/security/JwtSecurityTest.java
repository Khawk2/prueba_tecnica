package com.nequi.franchises.security;

import com.nequi.franchises.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security tests for JWT token handling.
 */
class JwtSecurityTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Use a test secret (32+ characters)
        jwtTokenProvider = new JwtTokenProvider(
                "test-secret-key-for-jwt-tokens-32chars", 
                86400000 // 24 hours
        );
    }

    @Test
    @DisplayName("Generate and validate token successfully")
    void generateAndValidateToken() {
        // Given
        String userId = "user-123";
        String username = "testuser";
        List<String> roles = List.of("USER", "ADMIN");

        // When
        String token = jwtTokenProvider.generateToken(userId, username, roles);
        Authentication auth = jwtTokenProvider.validateToken(token);

        // Then
        assertThat(token).isNotNull();
        assertThat(auth).isNotNull();
        assertThat(auth.getName()).isEqualTo(username);
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Invalid token should return null authentication")
    void invalidTokenReturnsNull() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        Authentication auth = jwtTokenProvider.validateToken(invalidToken);

        // Then
        assertThat(auth).isNull();
        assertThat(jwtTokenProvider.isValid(invalidToken)).isFalse();
    }

    @Test
    @DisplayName("Token with custom expiration should expire correctly")
    void tokenWithShortExpiration() throws InterruptedException {
        // Given - 1 second expiration
        String token = jwtTokenProvider.generateTokenWithExpiration(
                "user-123", 
                "testuser", 
                List.of("USER"), 
                0 // 0 minutes = expired immediately
        );

        // Small delay to ensure expiration
        TimeUnit.MILLISECONDS.sleep(100);

        // When/Then
        assertThat(jwtTokenProvider.isValid(token)).isFalse();
    }

    @Test
    @DisplayName("Extract userId from valid token")
    void extractUserIdFromToken() {
        // Given
        String userId = "user-456";
        String token = jwtTokenProvider.generateToken(userId, "testuser", List.of("USER"));

        // When
        String extractedUserId = jwtTokenProvider.getUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    @DisplayName("Tampered token should be invalid")
    void tamperedTokenIsInvalid() {
        // Given
        String token = jwtTokenProvider.generateToken("user-123", "testuser", List.of("USER"));
        String tamperedToken = token.substring(0, token.length() - 5) + "XXXXX";

        // When/Then
        assertThat(jwtTokenProvider.isValid(tamperedToken)).isFalse();
    }
}
