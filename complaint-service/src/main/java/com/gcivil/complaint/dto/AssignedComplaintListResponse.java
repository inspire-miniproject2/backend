package com.gcivil.complaint.dto;

import java.util.List;

public record AssignedComplaintListResponse(
        Summary summary,
        List<AssignedComplaintSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public record Summary(
            long newAssigned,
            long inProgress,
            long completed
    ) {
    }

    public record AssignedComplaintSummary(
            Long complaintId,
            String complaintNo,
            String title,
            String assigneeName,
            Long assigneeUserId,
            String status,
            Long assignedDepartmentId
    ) {
    }
}
