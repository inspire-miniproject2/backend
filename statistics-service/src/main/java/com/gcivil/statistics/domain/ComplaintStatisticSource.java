package com.gcivil.statistics.domain;

import java.time.LocalDate;

public record ComplaintStatisticSource(
        Long complaintId,
        LocalDate statisticDate,
        Long assignedDepartmentId,
        String categoryCode,
        String currentStatus
) {
}
