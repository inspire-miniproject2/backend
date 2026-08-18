package com.gcivil.complaint.event;

import java.time.OffsetDateTime;
import java.util.List;

public record ComplaintCreatedPayload(
        Long complaintId,
        String complaintNo,
        Long applicantUserId,
        Long categoryId,
        String categoryCode,
        String currentStatus,
        OffsetDateTime submittedAt,
        List<NotifyChannel> notifyChannels
) {
}
