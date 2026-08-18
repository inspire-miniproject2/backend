package com.gcivil.notification.application;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Long notificationId) {
        super("Notification not found: " + notificationId);
    }
}
