package com.gcivil.complaint.dto;

import java.time.LocalDateTime;

public record RegisterComplaintResponseResponse(
        Long responseId,
        Long complaintId,
        boolean isPublic,
        LocalDateTime respondedAt
) {
}
