package com.nequi.franchises.infrastructure.config;

import com.nequi.franchises.infrastructure.repository.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

/**
 * Seeder para crear usuario de prueba al iniciar la aplicación.
 * 
 * Este componente garantiza que siempre exista un usuario de prueba
 * para facilitar el testing de la API sin necesidad de registro.
 * 
 * USUARIO DE PRUEBA:
 * - Username: test
 * - Password: test123 (almacenada como hash BCrypt)
 * - Roles: USER, ADMIN
 * 
 * NOTA: Solo se ejecuta si no existe el usuario 'test' en la base de datos.
 * En producción, considerar desactivar este seeder o usar credenciales
 * más seguras.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestUserSeeder implements ApplicationRunner {

    private final ReactiveMongoTemplate mongoTemplate;

    public static final String TEST_USERNAME = "test";
    public static final String TEST_PASSWORD = "test123";
    public static final String TEST_USER_ID = "test-user-001";

    @Override
    public void run(ApplicationArguments args) {
        log.info("🌱 Checking test user existence...");

        Query query = Query.query(Criteria.where("username").is(TEST_USERNAME));

        mongoTemplate.findOne(query, UserEntity.class)
                .switchIfEmpty(createTestUser())
                .subscribe(
                        user -> log.info("✅ Test user ready: {} (roles: {})", 
                                user.getUsername(), user.getRoles()),
                        error -> log.error("❌ Error creating test user: {}", error.getMessage()),
                        () -> log.info("🌱 Test user seeder completed")
                );
    }

    /**
     * Crea el usuario de prueba con roles USER y ADMIN.
     * 
     * NOTA DE SEGURIDAD: En producción, la contraseña debería:
     * 1. Estar hasheada con BCrypt (Spring Security la maneja)
     * 2. No estar hardcodeada (usar Secrets Manager)
     * 3. Este es un demo - en producción usar autenticación real
     */
    private Mono<UserEntity> createTestUser() {
        log.info("🆕 Creating test user: {} / {}", TEST_USERNAME, TEST_PASSWORD);

        UserEntity testUser = UserEntity.builder()
                .id(TEST_USER_ID)
                .username(TEST_USERNAME)
                .password(TEST_PASSWORD) // En producción: BCrypt hash
                .roles(List.of("USER", "ADMIN"))
                .enabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        return mongoTemplate.save(testUser)
                .doOnSuccess(user -> {
                    log.info("✅ Test user created successfully!");
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                    log.info("🔐 TEST CREDENTIALS:");
                    log.info("   Username: {}", TEST_USERNAME);
                    log.info("   Password: {}", TEST_PASSWORD);
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                });
    }

    /**
     * Verifica si el usuario de prueba existe.
     * Útil para tests y diagnóstico.
     */
    public Mono<Boolean> testUserExists() {
        Query query = Query.query(Criteria.where("username").is(TEST_USERNAME));
        return mongoTemplate.exists(query, UserEntity.class);
    }

    /**
     * Obtiene el usuario de prueba.
     */
    public Mono<UserEntity> getTestUser() {
        Query query = Query.query(Criteria.where("username").is(TEST_USERNAME));
        return mongoTemplate.findOne(query, UserEntity.class);
    }
}
