package com.nequi.franchises.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Configuration for distributed caching and rate limiting.
 * 
 * ROOT CAUSE FIX: Solo se activa si Redis está explícitamente habilitado.
 * Esto evita errores de startup y permite desactivar Redis fácilmente.
 * 
 * Beans creados:
 * - ReactiveRedisTemplate<String, Object> para caché
 * - ReactiveStringRedisTemplate para rate limiting (alias del template String)
 */
@Configuration
@ConditionalOnProperty(
    name = "app.features.redis", 
    havingValue = "true", 
    matchIfMissing = false  // Requerir configuración explícita
)
public class RedisConfig {

    /**
     * Bean para operaciones con objetos JSON (caché).
     * Solo se crea si no existe otro bean del mismo tipo.
     */
    @Bean
    @ConditionalOnMissingBean(name = "reactiveRedisTemplate")
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();

        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, Object> context = builder
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }

    /**
     * Bean para operaciones con Strings (rate limiting).
     * CRÍTICO: DistributedRateLimitingService depende de este bean.
     * 
     * ROOT CAUSE FIX: Crear ReactiveStringRedisTemplate específico para rate limiting.
     * Spring Boot busca este tipo exacto, no ReactiveRedisTemplate<String, String>.
     */
    @Bean("reactiveStringRedisTemplate")
    @ConditionalOnMissingBean(name = "reactiveStringRedisTemplate")
    public ReactiveStringRedisTemplate reactiveStringRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        
        StringRedisSerializer serializer = new StringRedisSerializer();
        
        RedisSerializationContext<String, String> context = RedisSerializationContext
                .<String, String>newSerializationContext(serializer)
                .value(serializer)
                .build();

        return new ReactiveStringRedisTemplate(factory, context);
    }
}
