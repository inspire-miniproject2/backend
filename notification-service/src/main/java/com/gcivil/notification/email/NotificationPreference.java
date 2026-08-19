package com.gcivil.notification.email;

public record NotificationPreference(
        Long userId,
        String email,
        boolean emailNotifyAgreed,
        boolean isActive
) {
}
