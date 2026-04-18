package com.nequi.franchises.domain.event;

import java.time.Instant;

/**
 * Base interface for all domain events.
 * Domain events represent significant business occurrences.
 */
public interface DomainEvent {
    
    /**
     * Gets the unique identifier of the event.
     */
    String getEventId();
    
    /**
     * Gets the timestamp when the event occurred.
     */
    Instant getOccurredOn();
    
    /**
     * Gets the type of the event.
     */
    String getEventType();
}
