package com.gcivil.complaint.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class ComplaintKafkaProducer {
    private static final Logger log = LoggerFactory.getLogger(ComplaintKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    ComplaintKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void sendAfterCommit(ComplaintEventMessage<?> message) {
        var envelope = new EventEnvelope<>(
                message.eventId(),
                message.eventType(),
                ComplaintEventTypes.VERSION,
                message.occurredAt(),
                ComplaintEventTypes.PRODUCER,
                message.partitionKey(),
                message.payload());

        kafkaTemplate.send(message.topic(), message.partitionKey(), envelope)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish complaint event. eventId={}, topic={}",
                                message.eventId(), message.topic(), error);
                        return;
                    }
                    log.info("Published complaint event. eventId={}, topic={}, partition={}, offset={}",
                            message.eventId(), message.topic(), result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
