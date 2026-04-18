package com.nequi.franchises.domain.publisher;

import com.nequi.franchises.domain.event.DomainEvent;

/**
 * Port for publishing domain events.
 * Infrastructure will provide the adapter implementation.
 */
public interface DomainEventPublisher {
    
    /**
     * Publishes a domain event.
     * 
     * @param event the domain event to publish
     */
    void publish(DomainEvent event);
}
