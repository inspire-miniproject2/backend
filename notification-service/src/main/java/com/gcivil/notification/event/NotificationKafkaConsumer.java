package com.gcivil.notification.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaConsumer {
    private final NotificationEventHandler eventHandler;

    public NotificationKafkaConsumer(NotificationEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    @KafkaListener(
            topics = "${gcivil.kafka.topics.complaint-status-changed:complaint.status.changed.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void consumeStatusChanged(EventEnvelope<ComplaintStatusChangedPayload> event) {
        eventHandler.handleStatusChanged(event);
    }

    @KafkaListener(
            topics = "${gcivil.kafka.topics.complaint-response-registered:complaint.response.registered.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void consumeResponseRegistered(EventEnvelope<ComplaintResponseRegisteredPayload> event) {
        eventHandler.handleResponseRegistered(event);
    }
}
