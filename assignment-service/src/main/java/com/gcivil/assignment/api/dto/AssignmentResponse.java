package com.gcivil.assignment.api.dto;

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

    public static AssignmentResponse success(
            Long departmentId,
            String departmentName,
            Long officerUserId,
            String officerName,
            Long assignmentRuleId,
            LocalDateTime assignedAt
    ) {
        return new AssignmentResponse(
                true,
                departmentId,
                departmentName,
                officerUserId,
                officerName,
                assignmentRuleId,
                assignedAt,
                null,
                null
        );
    }

    public static AssignmentResponse miss(String reasonCode, String reasonMessage) {
        return new AssignmentResponse(
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                reasonCode,
                reasonMessage
        );
    }
}
