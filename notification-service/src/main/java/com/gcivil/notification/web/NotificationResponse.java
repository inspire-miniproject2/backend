package com.gcivil.notification.web;

import com.gcivil.notification.domain.Notification;
import com.gcivil.notification.domain.NotificationType;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

public record NotificationResponse(
        Long notificationId,
        Long complaintId,
        String complaintNo,
        NotificationType type,
        String title,
        String message,
        boolean read,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
) {
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getComplaintId(), notification.getComplaintNo(),
                notification.getType(), notification.getTitle(), notification.getMessage(),
                notification.isRead(), toKoreaOffset(notification.getCreatedAt()),
                toKoreaOffset(notification.getReadAt()));
    }

    private static OffsetDateTime toKoreaOffset(LocalDateTime utcDateTime) {
        if (utcDateTime == null) {
            return null;
        }
        return utcDateTime.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(KOREA_ZONE)
                .toOffsetDateTime();
    }
}
