package com.gcivil.statistics.event;

import java.time.OffsetDateTime;
import java.util.List;

public record ComplaintStatusChangedPayload(
        Long complaintId,
        String complaintNo,
        String previousStatus,
        String currentStatus,
        Long statusChangedByUserId,
        Long assignedDepartmentId,
        Long assignedOfficerUserId,
        OffsetDateTime statusChangedAt,
        List<NotifyChannel> notifyChannels
) {
}
