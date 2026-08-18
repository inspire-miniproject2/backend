package com.gcivil.complaint.dto;

import java.util.List;

public record MyComplaintListResponse(
        Summary summary,
        List<MyComplaintSummary> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public record Summary(
            long total,
            long received,
            long assigned,
            long inProgress,
            long completed
    ) {
    }

    public record MyComplaintSummary(
            Long complaintId,
            String complaintNo,
            String title,
            String categoryCode,
            String currentStatus,
            String assignedDepartmentName,
            Long assignedDepartmentId,
            Long assignedOfficerUserId
    ) {
    }
}
