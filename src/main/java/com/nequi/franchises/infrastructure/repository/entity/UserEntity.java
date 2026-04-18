package com.nequi.franchises.infrastructure.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * User entity for MongoDB storage.
 * Used by TestUserSeeder to create demo credentials.
 */
@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    @Id
    private String id;

    private String username;
    private String password;
    private List<String> roles;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
