package com.gcivil.notification.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationKafkaConsumer {
    private final NotificationEventHandler eventHandler;
    private final ObjectMapper objectMapper;

    public NotificationKafkaConsumer(NotificationEventHandler eventHandler, ObjectMapper objectMapper) {
        this.eventHandler = eventHandler;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${gcivil.kafka.topics.complaint-status-changed:complaint.status.changed.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void consumeStatusChanged(String message) throws JsonProcessingException {
        EventEnvelope<ComplaintStatusChangedPayload> event = objectMapper.readValue(
                message, new TypeReference<>() {});
        eventHandler.handleStatusChanged(event);
    }

    @KafkaListener(
            topics = "${gcivil.kafka.topics.complaint-response-registered:complaint.response.registered.v1}",
            groupId = "${spring.kafka.consumer.group-id:notification-service}")
    public void consumeResponseRegistered(String message) throws JsonProcessingException {
        EventEnvelope<ComplaintResponseRegisteredPayload> event = objectMapper.readValue(
                message, new TypeReference<>() {});
        eventHandler.handleResponseRegistered(event);
    }
}
