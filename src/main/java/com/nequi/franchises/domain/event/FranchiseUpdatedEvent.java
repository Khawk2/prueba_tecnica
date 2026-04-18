package com.nequi.franchises.domain.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a franchise is updated.
 */
@Getter
public class FranchiseUpdatedEvent implements DomainEvent {
    
    private final String eventId;
    private final Instant occurredOn;
    private final String franchiseId;
    private final String newName;
    
    public FranchiseUpdatedEvent(String franchiseId, String newName) {
        this.eventId = UUID.randomUUID().toString();
        this.occurredOn = Instant.now();
        this.franchiseId = franchiseId;
        this.newName = newName;
    }
    
    @Override
    public String getEventType() {
        return "franchise.updated";
    }
}
