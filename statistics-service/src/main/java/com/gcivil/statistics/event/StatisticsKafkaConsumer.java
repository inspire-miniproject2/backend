package com.gcivil.statistics.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class StatisticsKafkaConsumer {
    private final StatisticsEventHandler eventHandler;
    private final ObjectMapper objectMapper;

    public StatisticsKafkaConsumer(StatisticsEventHandler eventHandler, ObjectMapper objectMapper) {
        this.eventHandler = eventHandler;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${gcivil.kafka.topics.complaint-created:complaint.created.v1}",
            groupId = "${spring.kafka.consumer.group-id:statistics-service}")
    public void consumeCreated(String message) throws JsonProcessingException {
        EventEnvelope<ComplaintCreatedPayload> event = objectMapper.readValue(
                message, new TypeReference<>() {});
        eventHandler.handleCreated(event);
    }

    @KafkaListener(
            topics = "${gcivil.kafka.topics.complaint-status-changed:complaint.status.changed.v1}",
            groupId = "${spring.kafka.consumer.group-id:statistics-service}")
    public void consumeStatusChanged(String message) throws JsonProcessingException {
        EventEnvelope<ComplaintStatusChangedPayload> event = objectMapper.readValue(
                message, new TypeReference<>() {});
        eventHandler.handleStatusChanged(event);
    }
}
