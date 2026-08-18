package com.gcivil.complaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_status_history")
public class ComplaintStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ComplaintStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private ComplaintStatus newStatus;

    @Column(name = "changed_by_user_id")
    private Long changedByUserId;

    @Column(name = "change_memo", length = 255)
    private String changeMemo;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected ComplaintStatusHistory() {
    }

    public ComplaintStatusHistory(
            Complaint complaint,
            ComplaintStatus previousStatus,
            ComplaintStatus newStatus,
            Long changedByUserId,
            String changeMemo,
            LocalDateTime changedAt
    ) {
        this.complaint = complaint;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedByUserId = changedByUserId;
        this.changeMemo = changeMemo;
        this.changedAt = changedAt;
    }

    public ComplaintStatus getPreviousStatus() {
        return previousStatus;
    }

    public ComplaintStatus getNewStatus() {
        return newStatus;
    }

    public Long getChangedByUserId() {
        return changedByUserId;
    }

    public String getChangeMemo() {
        return changeMemo;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
