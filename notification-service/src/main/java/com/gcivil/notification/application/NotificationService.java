package com.gcivil.notification.application;

import com.gcivil.notification.domain.Notification;
import com.gcivil.notification.domain.NotificationRepository;
import com.gcivil.notification.web.NotificationPageResponse;
import com.gcivil.notification.web.NotificationResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final Clock clock;

    public NotificationService(NotificationRepository notificationRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public NotificationPageResponse findAll(Long userId, int page, int size, Boolean read) {
        var pageable = PageRequest.of(page, size);
        var result = read == null
                ? notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, read, pageable);
        return new NotificationPageResponse(
                notificationRepository.countByUserIdAndReadFalse(userId),
                result.getContent().stream().map(NotificationResponse::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        notification.markAsRead(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        return NotificationResponse.from(notification);
    }
}
