package com.gcivil.complaint.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ComplaintDetailResponse(
        Long complaintId,
        String complaintNo,
        Long applicantUserId,
        String categoryCode,
        String title,
        String content,
        String currentStatus,
        String assignedDepartmentName,
        Long assignedDepartmentId,
        Long assignedOfficerUserId,
        LocalDateTime submittedAt,
        LocalDateTime assignedAt,
        LocalDateTime completedAt,
        ResponseSummary response,
        List<AttachmentSummary> attachments,
        List<StatusHistorySummary> statusHistories
) {
    public record ResponseSummary(
            Long responseId,
            Long responderUserId,
            boolean isPublic,
            String responseContent,
            LocalDateTime respondedAt
    ) {
    }

    public record AttachmentSummary(
            Long attachmentId,
            String originalFilename,
            String contentType,
            Long fileSize,
            LocalDateTime uploadedAt
    ) {
    }

    public record StatusHistorySummary(
            String previousStatus,
            String newStatus,
            Long changedByUserId,
            String changeMemo,
            LocalDateTime changedAt
    ) {
    }
}
