package com.gcivil.notification.event;

import com.gcivil.notification.domain.Notification;
import com.gcivil.notification.domain.NotificationRepository;
import com.gcivil.notification.domain.NotificationType;
import com.gcivil.notification.domain.ProcessedEvent;
import com.gcivil.notification.domain.ProcessedEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventHandler {
    private final NotificationRepository notificationRepository;
    private final ProcessedEventRepository processedEventRepository;

    public NotificationEventHandler(NotificationRepository notificationRepository,
                                    ProcessedEventRepository processedEventRepository) {
        this.notificationRepository = notificationRepository;
        this.processedEventRepository = processedEventRepository;
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
        markProcessed(event);
    }

    private boolean isProcessed(EventEnvelope<?> event) {
        return processedEventRepository.existsById(event.eventId().toString());
    }

    private void markProcessed(EventEnvelope<?> event) {
        processedEventRepository.save(new ProcessedEvent(
                event.eventId(), event.eventType(), event.occurredAt().toLocalDateTime()));
    }
}
