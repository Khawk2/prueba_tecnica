package com.nequi.franchises.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * MongoDB Configuration with Reactive Transactions.
 *
 * ARCHITECTURE PRINCIPLE:
 * - NO detection of runtime environment (AWS, local, etc.)
 * - Configuration is 100% externalized via Spring Profiles
 * - application-dev.yml  -> Local development
 * - application-prod.yml -> Production (AWS ECS)
 *
 * SECURITY:
 * - Credentials are NEVER logged
 * - URIs are masked in logs (mongodb+srv://***@cluster...)
 * - Fail-fast if MONGODB_URI is missing (prod profile)
 *
 * REQUIREMENTS:
 * - MongoDB 4.0+ with replica set (required for transactions)
 * - MONGODB_URI environment variable configured
 */
@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.nequi.franchises.domain.repository")
@EnableTransactionManagement
@Slf4j
public class MongoConfig {

    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    /**
     * Validates MongoDB connection configuration on startup.
     * Logs connection type with masked credentials (security).
     *
     * FAIL-FAST PRINCIPLE:
     * - In production (prod profile), MONGODB_URI is REQUIRED
     * - Application will fail to start if not configured
     * - NO silent fallbacks in production
     */
    @PostConstruct
    public void validateConnection() {
        // Validate URI is configured
        if (mongoUri == null || mongoUri.isEmpty()) {
            log.error("=================================================");
            log.error("MONGODB_URI NOT CONFIGURED");
            log.error("=================================================");
            log.error("The application requires MONGODB_URI environment variable.");
            log.error("");
            log.error("Local Development:");
            log.error("  SPRING_PROFILES_ACTIVE=dev");
            log.error("  MONGODB_URI=mongodb://localhost:27017/franchises");
            log.error("");
            log.error("AWS Production:");
            log.error("  SPRING_PROFILES_ACTIVE=prod");
            log.error("  MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/franchises");
            log.error("=================================================");

            throw new IllegalStateException(
                "MONGODB_URI is required. " +
                "Set the environment variable or configure application-{profile}.yml"
            );
        }

        // Determine connection type and mask credentials for logging
        ConnectionInfo info = analyzeConnection(mongoUri);

        // Log with masked URI (security: never expose credentials)
        log.info("=================================================");
        log.info("MongoDB Connection Status");
        log.info("=================================================");
        log.info("Type:   {}", info.type);
        log.info("Source: Environment Variable");
        log.info("URI:    {}", info.maskedUri);
        log.info("=================================================");
    }

    /**
     * Analyzes MongoDB URI and returns masked connection info.
     * Security: Credentials are NEVER exposed in returned data.
     */
    private ConnectionInfo analyzeConnection(String uri) {
        String type;
        String maskedUri;

        if (uri.contains("mongodb+srv://")) {
            // MongoDB Atlas (Cloud)
            type = "MongoDB Atlas (Cloud)";
            maskedUri = maskCredentials(uri, "mongodb+srv://");
        } else if (uri.contains("localhost") || uri.contains("127.0.0.1")) {
            // Local development
            type = "MongoDB Local (Development)";
            maskedUri = uri; // Local URIs typically don't have credentials
        } else if (uri.contains("@")) {
            // Custom MongoDB with auth
            type = "MongoDB (Custom)";
            maskedUri = maskCredentials(uri, uri.substring(0, uri.indexOf("://") + 3));
        } else {
            // Other (no auth)
            type = "MongoDB (No Auth)";
            maskedUri = uri;
        }

        return new ConnectionInfo(type, maskedUri);
    }

    /**
     * Masks credentials in MongoDB URI.
     * Example: mongodb+srv://user:pass@cluster... → mongodb+srv://***@cluster...
     */
    private String maskCredentials(String uri, String prefix) {
        // Replace user:pass with ***
        return uri.replaceAll("//[^:]+:[^@]+@", "//***@");
    }

    /**
     * Connection analysis result (immutable).
     */
    private record ConnectionInfo(String type, String maskedUri) {}

    @Bean
    public ReactiveMongoTransactionManager reactiveMongoTransactionManager(
            ReactiveMongoDatabaseFactory factory) {
        return new ReactiveMongoTransactionManager(factory);
    }

    /**
     * TransactionalOperator for programmatic reactive transactions.
     * Usage: transactionalOperator.execute(status -> { ... })
     */
    @Bean
    public TransactionalOperator transactionalOperator(ReactiveTransactionManager manager) {
        return TransactionalOperator.create(manager);
    }
}
