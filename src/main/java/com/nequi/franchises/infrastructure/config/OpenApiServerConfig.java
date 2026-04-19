package com.nequi.franchises.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI Server Configuration.
 * 
 * This configuration dynamically sets the server URL for Swagger UI
 * based on the deployment environment (local dev vs AWS production).
 * 
 * RESOLVES: The "Failed to fetch" error when Swagger UI tries to call localhost
 * instead of the actual AWS ALB URL.
 */
@Configuration
public class OpenApiServerConfig {

    @Value("${app.server.url:http://localhost:8080}")
    private String serverUrl;

    /**
     * Configure OpenAPI with explicit server URL.
     * This ensures Swagger UI uses the correct base URL for API calls.
     * 
     * In AWS ECS, set APP_SERVER_URL environment variable to the ALB URL:
     * APP_SERVER_URL=http://franchises-api-1585613032.us-east-1.elb.amazonaws.com
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description("API Server")
                ));
    }
}
