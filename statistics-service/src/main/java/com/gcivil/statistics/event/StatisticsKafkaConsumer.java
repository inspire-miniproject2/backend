package com.gcivil.statistics.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class StatisticsKafkaConsumer {
    private final StatisticsEventHandler eventHandler;

    public StatisticsKafkaConsumer(StatisticsEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @KafkaListener(
            topics = "${gcivil.kafka.topics.complaint-created:complaint.created.v1}",
            groupId = "${spring.kafka.consumer.group-id:statistics-service}")
    public void consumeCreated(EventEnvelope<ComplaintCreatedPayload> event) {
        eventHandler.handleCreated(event);
    }

    @KafkaListener(
            topics = "${gcivil.kafka.topics.complaint-status-changed:complaint.status.changed.v1}",
            groupId = "${spring.kafka.consumer.group-id:statistics-service}")
    public void consumeStatusChanged(EventEnvelope<ComplaintStatusChangedPayload> event) {
        eventHandler.handleStatusChanged(event);
    }
}
