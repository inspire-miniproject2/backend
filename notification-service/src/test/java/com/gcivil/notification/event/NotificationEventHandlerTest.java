package com.gcivil.notification.event;

import com.gcivil.notification.domain.Notification;
import com.gcivil.notification.domain.NotificationRepository;
import com.gcivil.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class NotificationEventHandlerTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final NotificationEventHandler handler = new NotificationEventHandler(repository);

    @Test
    void savesInAppStatusNotificationForApplicant() {
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-15T09:31:00+09:00");
        var payload = new ComplaintStatusChangedPayload(
                1001L, "CIV-2026-000184", 501L, 10L, "ROAD", "RECEIVED", "ASSIGNED",
                21L, 9001L, null, occurredAt, null, null,
                List.of(NotifyChannel.IN_APP, NotifyChannel.EMAIL));
        var event = new EventEnvelope<>(UUID.randomUUID(), "ComplaintStatusChanged", "v1", occurredAt,
                "complaint-service", "1001", payload);

        handler.handleStatusChanged(event);

        var captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(501L);
        assertThat(captor.getValue().getComplaintId()).isEqualTo(1001L);
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.STATUS_CHANGED);
    }

    @Test
    void savesResponseNotificationForApplicant() {
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-15T10:00:00+09:00");
        var payload = new ComplaintResponseRegisteredPayload(
                1001L, "CIV-2026-000184", 7001L, 501L, 9001L, true,
                occurredAt, 21L, List.of(NotifyChannel.IN_APP));
        var event = new EventEnvelope<>(UUID.randomUUID(), "ComplaintResponseRegistered", "v1", occurredAt,
                "complaint-service", "1001", payload);

        handler.handleResponseRegistered(event);

        var captor = org.mockito.ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(501L);
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.RESPONSE_REGISTERED);
    }

    @Test
    void ignoresEventsWithoutInAppChannel() {
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-15T10:00:00+09:00");
        var payload = new ComplaintResponseRegisteredPayload(
                1001L, "CIV-2026-000184", 7001L, 501L, 9001L, true,
                occurredAt, 21L, List.of(NotifyChannel.EMAIL));

        handler.handleResponseRegistered(new EventEnvelope<>(UUID.randomUUID(),
                "ComplaintResponseRegistered", "v1", occurredAt, "complaint-service", "1001", payload));

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
