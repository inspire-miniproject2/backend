package com.gcivil.notification.event;

import java.time.OffsetDateTime;
import java.util.List;

public record ComplaintResponseRegisteredPayload(
        Long complaintId,
        String complaintNo,
        Long responseId,
        Long applicantUserId,
        Long responderUserId,
        boolean isPublic,
        OffsetDateTime respondedAt,
        Long assignedDepartmentId,
        List<NotifyChannel> notifyChannels
) {
}
