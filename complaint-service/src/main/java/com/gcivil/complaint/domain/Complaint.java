package com.gcivil.complaint.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "complaint_no", nullable = false, unique = true, length = 32)
    private String complaintNo;

    @Column(name = "applicant_user_id", nullable = false)
    private Long applicantUserId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    private ComplaintStatus currentStatus;

    @Column(name = "assigned_department_id")
    private Long assignedDepartmentId;

    @Column(name = "assigned_department_name", length = 100)
    private String assignedDepartmentName;

    @Column(name = "assigned_officer_user_id")
    private Long assignedOfficerUserId;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintAttachment> attachments = new ArrayList<>();

    protected Complaint() {
    }

    public Complaint(
            String complaintNo,
            Long applicantUserId,
            Long categoryId,
            String categoryCode,
            String title,
            String content,
            ComplaintStatus currentStatus,
            LocalDateTime submittedAt,
            LocalDateTime createdAt
    ) {
        this.complaintNo = complaintNo;
        this.applicantUserId = applicantUserId;
        this.categoryId = categoryId;
        this.categoryCode = categoryCode;
        this.title = title;
        this.content = content;
        this.currentStatus = currentStatus;
        this.submittedAt = submittedAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public void addAttachment(ComplaintAttachment attachment) {
        attachments.add(attachment);
        attachment.attachTo(this);
    }

    public void markAssigned(String departmentName, Long departmentId, Long officerUserId, LocalDateTime assignedAt) {
        this.assignedDepartmentName = departmentName;
        this.assignedDepartmentId = departmentId;
        this.assignedOfficerUserId = officerUserId;
        this.assignedAt = assignedAt;
        this.currentStatus = ComplaintStatus.ASSIGNED;
        this.updatedAt = assignedAt;
    }

    public void markInProgress(LocalDateTime changedAt) {
        this.currentStatus = ComplaintStatus.IN_PROGRESS;
        this.updatedAt = changedAt;
    }

    public void markCompleted(LocalDateTime changedAt) {
        this.currentStatus = ComplaintStatus.COMPLETED;
        this.completedAt = changedAt;
        this.updatedAt = changedAt;
    }

    public void touch(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getComplaintNo() {
        return complaintNo;
    }

    public Long getApplicantUserId() {
        return applicantUserId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public ComplaintStatus getCurrentStatus() {
        return currentStatus;
    }

    public Long getAssignedDepartmentId() {
        return assignedDepartmentId;
    }

    public String getAssignedDepartmentName() {
        return assignedDepartmentName;
    }

    public Long getAssignedOfficerUserId() {
        return assignedOfficerUserId;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<ComplaintAttachment> getAttachments() {
        return attachments;
    }
}
