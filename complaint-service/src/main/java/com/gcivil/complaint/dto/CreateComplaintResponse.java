package com.gcivil.complaint.dto;

import com.gcivil.complaint.domain.ComplaintStatus;
import java.time.LocalDateTime;

public record CreateComplaintResponse(
        Long complaintId,
        String complaintNo,
        String categoryCode,
        ComplaintStatus currentStatus,
        Long assignedDepartmentId,
        Long assignedOfficerUserId,
        LocalDateTime submittedAt
) {
}
