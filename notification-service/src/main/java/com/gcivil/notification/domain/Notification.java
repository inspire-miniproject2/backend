package com.gcivil.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, columnDefinition = "char(36)")
    private String eventId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "complaint_id", nullable = false)
    private Long complaintId;

    @Column(name = "complaint_no", nullable = false, length = 32)
    private String complaintNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected Notification() {
    }

    public Notification(UUID eventId, Long userId, Long complaintId, String complaintNo,
                        NotificationType type, String title, String message, OffsetDateTime createdAt) {
        this.eventId = eventId.toString();
        this.userId = userId;
        this.complaintId = complaintId;
        this.complaintNo = complaintNo;
        this.type = type;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt.toLocalDateTime();
    }

    public void markAsRead(LocalDateTime now) {
        if (!read) {
            read = true;
            readAt = now;
        }
    }

    public Long getId() { return id; }
    public UUID getEventId() { return UUID.fromString(eventId); }
    public Long getUserId() { return userId; }
    public Long getComplaintId() { return complaintId; }
    public String getComplaintNo() { return complaintNo; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
}
