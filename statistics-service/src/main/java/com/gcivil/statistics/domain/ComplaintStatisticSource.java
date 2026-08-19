package com.gcivil.statistics.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ComplaintStatisticSource(
        Long complaintId,
        LocalDate statisticDate,
        Long assignedDepartmentId,
        String categoryCode,
        String currentStatus,
        LocalDateTime submittedAt,
        LocalDateTime dueAt,
        LocalDateTime completedAt
) {
}
