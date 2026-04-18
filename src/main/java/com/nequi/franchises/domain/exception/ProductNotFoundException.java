package com.nequi.franchises.domain.exception;

/**
 * Exception thrown when a product is not found.
 * Uses String ID for MongoDB ObjectId compatibility.
 */
public class ProductNotFoundException extends DomainException {

    public ProductNotFoundException(String id) {
        super("Product not found with id: " + id);
    }
}
