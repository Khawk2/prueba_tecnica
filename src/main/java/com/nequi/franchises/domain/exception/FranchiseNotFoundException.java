package com.nequi.franchises.domain.exception;

/**
 * Exception thrown when a franchise is not found.
 * Uses String ID for MongoDB ObjectId compatibility.
 */
public class FranchiseNotFoundException extends DomainException {

    public FranchiseNotFoundException(String id) {
        super("Franchise not found with id: " + id);
    }
}
