package com.nequi.franchises.domain.exception;

/**
 * Exception thrown when a branch is not found.
 * Uses String ID for MongoDB ObjectId compatibility.
 */
public class BranchNotFoundException extends DomainException {

    public BranchNotFoundException(String id) {
        super("Branch not found with id: " + id);
    }
}
