package com.nequi.franchises.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT Token Provider for authentication.
 * 
 * RESPONSIBILITIES:
 * - Generate JWT tokens
 * - Validate tokens
 * - Extract claims and create Authentication
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("${spring.security.jwt.secret}") String secret,
            @Value("${spring.security.jwt.expiration:86400000}") long expiration) {
        // Ensure secret is at least 256 bits (32 characters)
        String paddedSecret = String.format("%-32s", secret).substring(0, 32);
        this.secretKey = Keys.hmacShaKeyFor(paddedSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expiration;
    }

    /**
     * Generate JWT token for user with default expiration.
     */
    public String generateToken(String userId, String username, List<String> roles) {
        return generateTokenWithExpiration(userId, username, roles, expirationMs / 60000);
    }

    /**
     * Generate JWT token with custom expiration in minutes.
     * Used for access tokens with short expiration (15 minutes).
     */
    public String generateTokenWithExpiration(String userId, String username, List<String> roles, long expirationMinutes) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Validate token and return Authentication.
     */
    public Authentication validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String username = claims.get("username", String.class);
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);

            Collection<? extends GrantedAuthority> authorities = roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            return new UsernamePasswordAuthenticationToken(
                    new JwtUserDetails(userId, username, authorities),
                    token,
                    authorities
            );
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if token is valid.
     */
    public boolean isValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extract userId from token.
     */
    public String getUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * User details for JWT authentication.
     * Implements UserDetails so auth.getName() returns username correctly.
     */
    public record JwtUserDetails(String userId, String username, 
                                  Collection<? extends GrantedAuthority> authorities) implements UserDetails {
        
        @Override
        public String getUsername() {
            return username;
        }
        
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }
        
        @Override
        public String getPassword() {
            return null; // JWT doesn't store password
        }
        
        @Override
        public boolean isAccountNonExpired() {
            return true;
        }
        
        @Override
        public boolean isAccountNonLocked() {
            return true;
        }
        
        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }
        
        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
