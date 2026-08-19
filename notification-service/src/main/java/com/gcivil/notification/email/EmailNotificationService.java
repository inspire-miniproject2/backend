package com.gcivil.notification.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {
    private final NotificationPreferenceClient preferenceClient;
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public EmailNotificationService(
            NotificationPreferenceClient preferenceClient,
            JavaMailSender mailSender,
            @Value("${notifications.email.enabled:false}") boolean enabled,
            @Value("${notifications.email.from:no-reply@minwonon.local}") String from) {
        this.preferenceClient = preferenceClient;
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
    }

    public void sendStatusChanged(Long userId, String complaintNo, String previousStatus, String currentStatus) {
        sendIfAgreed(userId, "[민원온] 민원 상태 변경 안내",
                "민원 " + complaintNo + "의 상태가 " + previousStatus + "에서 " + currentStatus + "(으)로 변경되었습니다.");
    }

    public void sendResponseRegistered(Long userId, String complaintNo) {
        sendIfAgreed(userId, "[민원온] 민원 답변 등록 안내",
                "민원 " + complaintNo + "에 답변이 등록되었습니다. 민원온에서 확인해 주세요.");
    }

    private void sendIfAgreed(Long userId, String subject, String text) {
        if (!enabled) {
            return;
        }
        NotificationPreference preference = preferenceClient.getPreference(userId);
        if (!preference.isActive() || !preference.emailNotifyAgreed()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(preference.email());
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
