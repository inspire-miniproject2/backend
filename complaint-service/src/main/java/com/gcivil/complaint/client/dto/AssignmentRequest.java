package com.gcivil.complaint.client.dto;

import java.time.LocalDateTime;

public record AssignmentRequest(
        Long complaintId,
        String complaintNo,
        Long categoryId,
        String categoryCode,
        Long applicantUserId,
        LocalDateTime submittedAt
) {
}
