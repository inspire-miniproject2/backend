package com.gcivil.notification.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceNotificationPreferenceClient implements NotificationPreferenceClient {
    private final RestClient restClient;

    public UserServiceNotificationPreferenceClient(
            RestClient.Builder builder,
            @Value("${clients.user-service.url:http://localhost:8084}") String userServiceUrl) {
        this.restClient = builder.baseUrl(userServiceUrl).build();
    }

    @Override
    public NotificationPreference getPreference(Long userId) {
        PreferenceEnvelope response = restClient.get()
                .uri("/api/v1/internal/users/{userId}/notification-preference", userId)
                .header("X-Internal-Caller", "notification-service")
                .retrieve()
                .body(PreferenceEnvelope.class);
        if (response == null || !response.success() || response.data() == null) {
            throw new IllegalStateException("User Service returned an invalid notification preference response");
        }
        return response.data();
    }

    private record PreferenceEnvelope(boolean success, NotificationPreference data, String message) {
    }
}
