package com.nequi.franchises.application.util;

import com.nequi.franchises.domain.exception.ValidationException;
import reactor.core.publisher.Mono;

/**
 * Common validation utilities for reactive services.
 * 
 * Provides reusable validation methods that return Mono.error for reactive chains.
 */
public class ValidationUtils {

    private ValidationUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates that a string is not null and not blank.
     * 
     * @param value the string to validate
     * @param fieldName name of the field for error message
     * @return Mono.empty() if valid, Mono.error if invalid
     */
    public static Mono<Void> validateNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return Mono.error(new ValidationException(fieldName + " is required"));
        }
        return Mono.empty();
    }

    /**
     * Validates that a string is not null, not blank and matches minimum length.
     * 
     * @param value the string to validate
     * @param fieldName name of the field for error message
     * @param minLength minimum allowed length
     * @return Mono.empty() if valid, Mono.error if invalid
     */
    public static Mono<Void> validateMinLength(String value, String fieldName, int minLength) {
        if (value == null || value.isBlank()) {
            return Mono.error(new ValidationException(fieldName + " is required"));
        }
        if (value.trim().length() < minLength) {
            return Mono.error(new ValidationException(fieldName + " must be at least " + minLength + " characters"));
        }
        return Mono.empty();
    }

    /**
     * Validates that a number is positive (greater than 0).
     * 
     * @param value the number to validate
     * @param fieldName name of the field for error message
     * @return Mono.empty() if valid, Mono.error if invalid
     */
    public static Mono<Void> validatePositive(Integer value, String fieldName) {
        if (value == null || value <= 0) {
            return Mono.error(new ValidationException(fieldName + " must be greater than 0"));
        }
        return Mono.empty();
    }

    /**
     * Validates that a number is not negative (>= 0).
     * 
     * @param value the number to validate
     * @param fieldName name of the field for error message
     * @return Mono.empty() if valid, Mono.error if invalid
     */
    public static Mono<Void> validateNotNegative(Integer value, String fieldName) {
        if (value == null || value < 0) {
            return Mono.error(new ValidationException(fieldName + " cannot be negative"));
        }
        return Mono.empty();
    }

    /**
     * Validates that an object is not null.
     * 
     * @param value the object to validate
     * @param fieldName name of the field for error message
     * @return Mono.empty() if valid, Mono.error if invalid
     */
    public static Mono<Void> validateNotNull(Object value, String fieldName) {
        if (value == null) {
            return Mono.error(new ValidationException(fieldName + " is required"));
        }
        return Mono.empty();
    }

    /**
     * Combines multiple validations into a single Mono.
     * All validations are executed sequentially.
     * 
     * @param validations array of validation Monos
     * @return Mono.empty() if all valid, first Mono.error encountered
     */
    @SafeVarargs
    public static Mono<Void> validateAll(Mono<Void>... validations) {
        Mono<Void> result = Mono.empty();
        for (Mono<Void> validation : validations) {
            result = result.then(validation);
        }
        return result;
    }
}
