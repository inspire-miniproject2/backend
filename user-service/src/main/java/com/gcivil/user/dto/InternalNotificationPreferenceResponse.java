package com.gcivil.user.dto;

public record InternalNotificationPreferenceResponse(
        Long userId,
        String email,
        boolean emailNotifyAgreed,
        boolean isActive
) {
}
