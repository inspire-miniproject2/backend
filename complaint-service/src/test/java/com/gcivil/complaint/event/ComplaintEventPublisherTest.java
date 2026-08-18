package com.gcivil.complaint.event;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ComplaintEventPublisherTest {
    private final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);
    private final ComplaintEventPublisher publisher = new ComplaintEventPublisher(
            applicationEventPublisher, "complaint.created.v1", "complaint.status.changed.v1",
            "complaint.response.registered.v1");

    @Test
    void publishesCreatedApplicationEventWithComplaintPartitionKey() {
        OffsetDateTime submittedAt = OffsetDateTime.parse("2026-08-15T09:30:00+09:00");
        var payload = new ComplaintCreatedPayload(
                1001L, "CIV-2026-000184", 501L, 10L, "ROAD", "RECEIVED",
                submittedAt, List.of(NotifyChannel.IN_APP));

        var eventId = publisher.publishComplaintCreated(payload);

        var captor = org.mockito.ArgumentCaptor.forClass(ComplaintEventMessage.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        ComplaintEventMessage<?> message = captor.getValue();
        assertThat(message.eventId()).isEqualTo(eventId);
        assertThat(message.topic()).isEqualTo("complaint.created.v1");
        assertThat(message.eventType()).isEqualTo(ComplaintEventTypes.COMPLAINT_CREATED);
        assertThat(message.partitionKey()).isEqualTo("1001");
        assertThat(message.payload()).isEqualTo(payload);
    }

    @Test
    void publishesAllSupportedEventKinds() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-15T10:00:00+09:00");
        publisher.publishComplaintStatusChanged(new ComplaintStatusChangedPayload(
                1001L, "CIV-2026-000184", "RECEIVED", "ASSIGNED", null,
                21L, 9001L, now, List.of(NotifyChannel.IN_APP)));
        publisher.publishComplaintResponseRegistered(new ComplaintResponseRegisteredPayload(
                1001L, "CIV-2026-000184", 7001L, 501L, 9001L, true,
                now, 21L, List.of(NotifyChannel.EMAIL)));

        verify(applicationEventPublisher, org.mockito.Mockito.times(2)).publishEvent(any(Object.class));
    }
}
