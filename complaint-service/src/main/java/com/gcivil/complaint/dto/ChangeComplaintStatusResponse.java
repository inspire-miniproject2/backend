package com.gcivil.complaint.dto;

import com.gcivil.complaint.domain.ComplaintStatus;
import java.time.LocalDateTime;

public record ChangeComplaintStatusResponse(
        ComplaintStatus previousStatus,
        ComplaintStatus newStatus,
        LocalDateTime changedAt
) {
}
