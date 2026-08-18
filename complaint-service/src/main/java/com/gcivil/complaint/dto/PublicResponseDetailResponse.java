package com.gcivil.complaint.dto;

import java.time.LocalDate;
import java.util.List;

public record PublicResponseDetailResponse(
        String caseTitle,
        String categoryName,
        LocalDate appliedDate,
        String departmentName,
        LocalDate completedAt,
        String responseContent,
        List<AttachmentSummary> attachments
) {
    public record AttachmentSummary(
            Long attachmentId,
            String originalFilename
    ) {
    }
}
