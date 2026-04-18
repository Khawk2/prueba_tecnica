package com.nequi.franchises.domain.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a new franchise is created.
 */
@Getter
public class FranchiseCreatedEvent implements DomainEvent {
    
    private final String eventId;
    private final Instant occurredOn;
    private final String franchiseId;
    private final String franchiseName;
    
    public FranchiseCreatedEvent(String franchiseId, String franchiseName) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
        this.franchiseId = franchiseId;
        this.franchiseName = franchiseName;
    }
    
    @Override
    public String getEventType() {
        return "franchise.created";
    }
}
