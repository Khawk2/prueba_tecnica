package com.nequi.franchises.infrastructure.config;

import com.nequi.franchises.infrastructure.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

/**
 * Security configuration for WebFlux reactive application.
 * 
 * FEATURES:
 * - JWT-based stateless authentication
 * - Role-based access control (RBAC)
 * - Stateless session management
 * - Public endpoints for auth and health
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Stateless - no sessions
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                
                // Disable CSRF (stateless JWT)
                .csrf(csrf -> csrf.disable())
                
                // Disable form login and HTTP basic
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                
                // Configure authorization
                .authorizeExchange(exchanges -> exchanges
                        // Public endpoints
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Swagger/OpenAPI - Público para documentación y pruebas
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        
                        // Admin endpoints
                        .pathMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        
                        // All other endpoints require authentication
                        .anyExchange().authenticated()
                )
                
                // Add JWT filter
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                
                .build();
    }
}
