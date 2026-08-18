package com.gcivil.complaint.dto;

import java.time.LocalDate;
import java.util.List;

public record PublicResponseListResponse(
        List<PublicResponseSummary> content
) {
    public record PublicResponseSummary(
            Long responseId,
            String title,
            String departmentName,
            LocalDate completedAt,
            String statusLabel
    ) {
    }
}
