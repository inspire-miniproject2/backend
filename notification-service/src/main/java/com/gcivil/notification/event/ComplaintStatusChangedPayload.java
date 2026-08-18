package com.gcivil.notification.event;

import java.time.OffsetDateTime;
import java.util.List;

public record ComplaintStatusChangedPayload(
        Long complaintId,
        String complaintNo,
        Long applicantUserId,
        Long categoryId,
        String categoryCode,
        String previousStatus,
        String currentStatus,
        Long assignedDepartmentId,
        Long assignedOfficerUserId,
        Long statusChangedByUserId,
        OffsetDateTime statusChangedAt,
        String changeMemo,
        OffsetDateTime respondedAt,
        List<NotifyChannel> notifyChannels
) {
}
