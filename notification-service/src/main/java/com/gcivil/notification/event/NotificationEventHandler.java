package com.gcivil.notification.event;

import com.gcivil.notification.domain.Notification;
import com.gcivil.notification.domain.NotificationRepository;
import com.gcivil.notification.domain.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventHandler {
    private final NotificationRepository notificationRepository;

    public NotificationEventHandler(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void handleStatusChanged(EventEnvelope<ComplaintStatusChangedPayload> event) {
        var payload = event.payload();
        if (!payload.notifyChannels().contains(NotifyChannel.IN_APP)) {
            return;
        }
        notificationRepository.save(new Notification(
                event.eventId(), payload.applicantUserId(), payload.complaintId(), payload.complaintNo(),
                NotificationType.STATUS_CHANGED, "민원 상태가 변경되었습니다.",
                payload.previousStatus() + " → " + payload.currentStatus(), event.occurredAt()));
    }

    @Transactional
    public void handleResponseRegistered(EventEnvelope<ComplaintResponseRegisteredPayload> event) {
        var payload = event.payload();
        if (!payload.notifyChannels().contains(NotifyChannel.IN_APP)) {
            return;
        }
        notificationRepository.save(new Notification(
                event.eventId(), payload.applicantUserId(), payload.complaintId(), payload.complaintNo(),
                NotificationType.RESPONSE_REGISTERED, "민원 답변이 등록되었습니다.",
                "민원 " + payload.complaintNo() + "에 답변이 등록되었습니다.", event.occurredAt()));
    }
}
