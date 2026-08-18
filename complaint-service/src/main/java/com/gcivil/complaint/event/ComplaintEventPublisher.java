package com.gcivil.complaint.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class ComplaintEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final String complaintCreatedTopic;
    private final String complaintStatusChangedTopic;
    private final String complaintResponseRegisteredTopic;

    public ComplaintEventPublisher(
            ApplicationEventPublisher applicationEventPublisher,
            @Value("${gcivil.kafka.topics.complaint-created:complaint.created.v1}") String complaintCreatedTopic,
            @Value("${gcivil.kafka.topics.complaint-status-changed:complaint.status.changed.v1}") String complaintStatusChangedTopic,
            @Value("${gcivil.kafka.topics.complaint-response-registered:complaint.response.registered.v1}") String complaintResponseRegisteredTopic
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.complaintCreatedTopic = complaintCreatedTopic;
        this.complaintStatusChangedTopic = complaintStatusChangedTopic;
        this.complaintResponseRegisteredTopic = complaintResponseRegisteredTopic;
    }

    public UUID publishComplaintCreated(ComplaintCreatedPayload payload) {
        return publish(complaintCreatedTopic, ComplaintEventTypes.COMPLAINT_CREATED,
                payload.submittedAt(), payload.complaintId(), payload);
    }

    public UUID publishComplaintStatusChanged(ComplaintStatusChangedPayload payload) {
        return publish(complaintStatusChangedTopic, ComplaintEventTypes.COMPLAINT_STATUS_CHANGED,
                payload.statusChangedAt(), payload.complaintId(), payload);
    }

    public UUID publishComplaintResponseRegistered(ComplaintResponseRegisteredPayload payload) {
        return publish(complaintResponseRegisteredTopic, ComplaintEventTypes.COMPLAINT_RESPONSE_REGISTERED,
                payload.respondedAt(), payload.complaintId(), payload);
    }

    private <T> UUID publish(String topic, String eventType, OffsetDateTime occurredAt, Long complaintId, T payload) {
        UUID eventId = UUID.randomUUID();
        applicationEventPublisher.publishEvent(new ComplaintEventMessage<>(
                eventId, topic, eventType, occurredAt, complaintId.toString(), payload));
        return eventId;
    }
}
