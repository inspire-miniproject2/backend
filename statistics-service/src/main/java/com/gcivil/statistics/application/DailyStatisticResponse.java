package com.gcivil.statistics.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyStatisticResponse(
        LocalDate statisticDate,
        long newReceivedCount,
        long newCompletedCount,
        long receivedStatusCount,
        long assignedStatusCount,
        long inProgressStatusCount,
        long completedStatusCount,
        long deadlineApproachingCount,
        long overdueCount,
        BigDecimal averageProcessingHours
) {
}
