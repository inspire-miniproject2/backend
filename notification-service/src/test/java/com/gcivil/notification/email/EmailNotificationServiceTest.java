package com.gcivil.notification.email;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailNotificationServiceTest {
    private final NotificationPreferenceClient preferenceClient = mock(NotificationPreferenceClient.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void sendsEmailOnlyForActiveAgreedUser() {
        when(preferenceClient.getPreference(501L))
                .thenReturn(new NotificationPreference(501L, "citizen@example.com", true, true));
        var service = new EmailNotificationService(
                preferenceClient, mailSender, true, "no-reply@example.com");

        service.sendResponseRegistered(501L, "CIV-2026-000184");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void skipsEmailWhenUserDidNotAgree() {
        when(preferenceClient.getPreference(501L))
                .thenReturn(new NotificationPreference(501L, "citizen@example.com", false, true));
        var service = new EmailNotificationService(
                preferenceClient, mailSender, true, "no-reply@example.com");

        service.sendStatusChanged(501L, "CIV-2026-000184", "RECEIVED", "ASSIGNED");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void doesNotCallUserServiceWhenEmailFeatureIsDisabled() {
        var service = new EmailNotificationService(
                preferenceClient, mailSender, false, "no-reply@example.com");

        service.sendResponseRegistered(501L, "CIV-2026-000184");

        verify(preferenceClient, never()).getPreference(501L);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
