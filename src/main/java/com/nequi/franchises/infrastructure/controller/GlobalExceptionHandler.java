package com.nequi.franchises.infrastructure.controller;

import com.nequi.franchises.domain.exception.*;
import com.nequi.franchises.infrastructure.config.CorrelationIdFilter;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Global exception handler for REST API.
 * Provides structured error responses with correlation ID for observability.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FranchiseNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleFranchiseNotFound(
            FranchiseNotFoundException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(BranchNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBranchNotFound(
            BranchNotFoundException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleProductNotFound(
            ProductNotFoundException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(
            ValidationException ex, ServerWebExchange exchange) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), exchange, null);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolation(
            ConstraintViolationException ex, ServerWebExchange exchange) {
        
        List<ValidationError> validationErrors = ex.getConstraintViolations().stream()
                .map(violation -> new ValidationError(
                        violation.getPropertyPath().toString(),
                        violation.getInvalidValue(),
                        violation.getMessage()
                ))
                .collect(Collectors.toList());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                exchange,
                validationErrors
        );
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServerWebInput(
            ServerWebInputException ex, ServerWebExchange exchange) {
        String message = ex.getReason() != null ? ex.getReason() : "Invalid input";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, exchange, null);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(
            Exception ex, ServerWebExchange exchange) {
        log.error("Unexpected error at {}: {}", 
                exchange.getRequest().getPath(), ex.getMessage(), ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                exchange,
                null
        );
    }

    private Mono<ResponseEntity<ErrorResponse>> buildErrorResponse(
            HttpStatus status,
            String message,
            ServerWebExchange exchange,
            List<ValidationError> validationErrors) {
        
        String path = exchange.getRequest().getPath().value();
        
        return Mono.deferContextual(contextView -> {
            String correlationId = getCorrelationId(contextView);
            
            ErrorResponse response = new ErrorResponse(
                    status.value(),
                    message,
                    path,
                    LocalDateTime.now(),
                    correlationId,
                    validationErrors
            );
            
            return Mono.just(ResponseEntity.status(status).body(response));
        });
    }

    private String getCorrelationId(ContextView contextView) {
        return contextView.getOrDefault(CorrelationIdFilter.CORRELATION_ID_KEY, "unknown");
    }

    /**
     * Complete error response with observability data.
     */
    public record ErrorResponse(
            int status,
            String message,
            String path,
            LocalDateTime timestamp,
            String requestId,
            List<ValidationError> validationErrors
    ) {}

    /**
     * Validation error detail.
     */
    public record ValidationError(
            String field,
            Object rejectedValue,
            String message
    ) {}
}
