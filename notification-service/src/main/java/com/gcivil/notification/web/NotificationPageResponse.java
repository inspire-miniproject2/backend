package com.gcivil.notification.web;

import java.util.List;

public record NotificationPageResponse(
        long unreadCount,
        List<NotificationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
