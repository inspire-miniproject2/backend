package com.gcivil.notification.web;

import com.gcivil.notification.domain.Notification;
import com.gcivil.notification.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long notificationId,
        Long complaintId,
        String complaintNo,
        NotificationType type,
        String title,
        String message,
        boolean read,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getComplaintId(), notification.getComplaintNo(),
                notification.getType(), notification.getTitle(), notification.getMessage(),
                notification.isRead(), notification.getCreatedAt(), notification.getReadAt());
    }
}
