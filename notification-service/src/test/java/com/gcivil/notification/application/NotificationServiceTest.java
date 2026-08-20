package com.gcivil.notification.application;

import com.gcivil.notification.domain.Notification;
import com.gcivil.notification.domain.NotificationRepository;
import com.gcivil.notification.domain.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationServiceTest {
    private final NotificationRepository repository = mock(NotificationRepository.class);
    private final Clock clock = Clock.fixed(
            Instant.parse("2026-08-15T02:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final NotificationService service = new NotificationService(repository, clock);

    @Test
    void marksOnlyOwnedNotificationAsRead() {
        var notification = new Notification(
                UUID.randomUUID(), 501L, 1001L, "CIV-2026-000184",
                NotificationType.RESPONSE_REGISTERED, "title", "message",
                OffsetDateTime.parse("2026-08-15T10:00:00+09:00"));
        when(repository.findByIdAndUserId(10L, 501L)).thenReturn(Optional.of(notification));

        var response = service.markAsRead(501L, 10L);

        assertThat(response.read()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo("2026-08-15T02:00:00");
        assertThat(response.createdAt())
                .isEqualTo(OffsetDateTime.parse("2026-08-15T10:00:00+09:00"));
        assertThat(response.readAt())
                .isEqualTo(OffsetDateTime.parse("2026-08-15T11:00:00+09:00"));
    }

    @Test
    void hidesNotificationsOwnedByAnotherUser() {
        when(repository.findByIdAndUserId(10L, 501L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(501L, 10L))
                .isInstanceOf(NotificationNotFoundException.class);
    }
}
