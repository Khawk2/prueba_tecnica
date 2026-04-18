package com.nequi.franchises.infrastructure.publisher;

import com.nequi.franchises.domain.event.DomainEvent;
import com.nequi.franchises.domain.publisher.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Simple implementation of DomainEventPublisher that logs events.
 * In production, this would publish to a message broker (SNS, Kafka, etc.)
 */
@Slf4j
@Component
public class LoggingDomainEventPublisher implements DomainEventPublisher {
    
    @Override
    public void publish(DomainEvent event) {
        log.info("Domain event published: type={}, id={}, occurredOn={}", 
                event.getEventType(), 
                event.getEventId(), 
                event.getOccurredOn());
        
        // TODO: In production, publish to SNS/SQS/Kafka
        // Example:
        // snsClient.publish(PublishRequest.builder()
        //     .topicArn(eventTopicArn)
        //     .message(objectMapper.writeValueAsString(event))
        //     .build());
    }
}
