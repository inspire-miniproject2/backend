package com.gcivil.complaint.client.dto;

import java.time.LocalDateTime;

public record AssignmentResponse(
        boolean assignmentFound,
        Long departmentId,
        String departmentName,
        Long officerUserId,
        String officerName,
        Long assignmentRuleId,
        LocalDateTime assignedAt,
        String reasonCode,
        String reasonMessage
) {
}
