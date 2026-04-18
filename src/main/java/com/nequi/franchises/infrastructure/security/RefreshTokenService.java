package com.nequi.franchises.infrastructure.security;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing refresh tokens.
 * 
 * SECURITY FEATURES:
 * - Access tokens: 15 minutes expiration
 * - Refresh tokens: 7 days expiration
 * - Refresh tokens stored in MongoDB (can be revoked)
 * - One-time use: refresh token invalidated after use
 * - Rotation: new refresh token issued with each access token refresh
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final ReactiveMongoTemplate mongoTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    
    // 7 days in seconds
    private static final long REFRESH_TOKEN_TTL_DAYS = 7;
    private static final long REFRESH_TOKEN_TTL_SECONDS = 60 * 60 * 24 * REFRESH_TOKEN_TTL_DAYS;

    /**
     * Create refresh token for user.
     */
    public Mono<RefreshToken> createRefreshToken(String userId, String username) {
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .username(username)
                .token(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(REFRESH_TOKEN_TTL_SECONDS))
                .used(false)
                .build();

        return mongoTemplate.save(token)
                .doOnSuccess(t -> log.info("Refresh token created for user: {}", userId));
    }

    /**
     * Validate refresh token and issue new tokens.
     * Implements token rotation: old token invalidated, new one issued.
     */
    public Mono<TokenPair> refreshAccessToken(String refreshTokenValue) {
        return mongoTemplate.findOne(
                Query.query(Criteria.where("token").is(refreshTokenValue)),
                RefreshToken.class)
                .flatMap(token -> {
                    // Check if token is expired
                    if (token.getExpiresAt().isBefore(Instant.now())) {
                        log.warn("Refresh token expired: {}", token.getId());
                        return Mono.error(new SecurityException("Refresh token expired"));
                    }

                    // Check if token was already used
                    if (token.isUsed()) {
                        log.warn("Refresh token reuse detected - possible attack: {}", token.getId());
                        // Revoke all tokens for this user (security measure)
                        return revokeAllUserTokens(token.getUserId())
                                .then(Mono.error(new SecurityException("Token reuse detected. All tokens revoked.")));
                    }

                    // Mark token as used
                    token.setUsed(true);
                    
                    // Generate new token pair (rotation)
                    String newAccessToken = jwtTokenProvider.generateToken(
                            token.getUserId(), 
                            token.getUsername(), 
                            java.util.List.of("USER")
                    );

                    return mongoTemplate.save(token)
                            .flatMap(saved -> createRefreshToken(token.getUserId(), token.getUsername()))
                            .map(newRefresh -> new TokenPair(newAccessToken, newRefresh.getToken()));
                })
                .switchIfEmpty(Mono.error(new SecurityException("Invalid refresh token")));
    }

    /**
     * Revoke all tokens for user.
     */
    public Mono<Void> revokeAllUserTokens(String userId) {
        Query query = Query.query(Criteria.where("userId").is(userId));
        return mongoTemplate.remove(query, RefreshToken.class).then();
    }

    /**
     * Revoke specific token.
     */
    public Mono<Void> revokeToken(String tokenValue) {
        Query query = Query.query(Criteria.where("token").is(tokenValue));
        return mongoTemplate.remove(query, RefreshToken.class).then();
    }

    /**
     * Token pair for authentication response.
     */
    public record TokenPair(String accessToken, String refreshToken) {}

    /**
     * Refresh token entity.
     */
    @Document(collection = "refresh_tokens")
    @Data
    @Builder
    public static class RefreshToken {
        @Id
        private String id;
        private String userId;
        private String username;
        private String token;
        private Instant createdAt;
        private Instant expiresAt;
        private boolean used;
    }
}
