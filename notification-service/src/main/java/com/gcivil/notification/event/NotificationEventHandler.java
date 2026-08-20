package com.gcivil.notification.event;

import com.gcivil.notification.domain.Notification;
import com.gcivil.notification.domain.NotificationRepository;
import com.gcivil.notification.domain.NotificationType;
import com.gcivil.notification.domain.ProcessedEvent;
import com.gcivil.notification.domain.ProcessedEventRepository;
import com.gcivil.notification.email.EmailNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;

@Service
public class NotificationEventHandler {
    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final EmailNotificationService emailNotificationService;

    public NotificationEventHandler(NotificationRepository notificationRepository,
                                    ProcessedEventRepository processedEventRepository,
                                    EmailNotificationService emailNotificationService) {
        this.notificationRepository = notificationRepository;
        this.processedEventRepository = processedEventRepository;
        this.emailNotificationService = emailNotificationService;
    }

    @Transactional
    public void handleStatusChanged(EventEnvelope<ComplaintStatusChangedPayload> event) {
        if (isProcessed(event)) {
            return;
        }
        var payload = event.payload();
        if (payload.notifyChannels().contains(NotifyChannel.IN_APP)) {
            notificationRepository.save(new Notification(
                    event.eventId(), payload.applicantUserId(), payload.complaintId(), payload.complaintNo(),
                    NotificationType.STATUS_CHANGED, "민원 상태가 변경되었습니다.",
                    payload.previousStatus() + " → " + payload.currentStatus(), event.occurredAt()));
        }
        if (payload.notifyChannels().contains(NotifyChannel.EMAIL)) {
            emailNotificationService.sendStatusChanged(
                    payload.applicantUserId(), payload.complaintNo(),
                    payload.previousStatus(), payload.currentStatus());
        }
        markProcessed(event);
    }

    @Transactional
    public void handleResponseRegistered(EventEnvelope<ComplaintResponseRegisteredPayload> event) {
        if (isProcessed(event)) {
            return;
        }
        var payload = event.payload();
        if (payload.notifyChannels().contains(NotifyChannel.IN_APP)) {
            notificationRepository.save(new Notification(
                    event.eventId(), payload.applicantUserId(), payload.complaintId(), payload.complaintNo(),
                    NotificationType.RESPONSE_REGISTERED, "민원 답변이 등록되었습니다.",
                    "민원 " + payload.complaintNo() + "에 답변이 등록되었습니다.", event.occurredAt()));
        }
        if (payload.notifyChannels().contains(NotifyChannel.EMAIL)) {
            emailNotificationService.sendResponseRegistered(
                    payload.applicantUserId(), payload.complaintNo());
        }
        markProcessed(event);
    }

    private boolean isProcessed(EventEnvelope<?> event) {
        return processedEventRepository.existsById(event.eventId().toString());
    }

    private void markProcessed(EventEnvelope<?> event) {
        processedEventRepository.save(new ProcessedEvent(
                event.eventId(), event.eventType(),
                event.occurredAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime()));
    }
}
