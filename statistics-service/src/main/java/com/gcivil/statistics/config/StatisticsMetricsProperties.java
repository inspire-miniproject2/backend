package com.gcivil.statistics.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "statistics.metrics")
public class StatisticsMetricsProperties {
    private int processingDeadlineDays = 7;
    private int deadlineApproachingDays = 2;

    public int getProcessingDeadlineDays() {
        return processingDeadlineDays;
    }

    public void setProcessingDeadlineDays(int processingDeadlineDays) {
        if (processingDeadlineDays < 1) {
            throw new IllegalArgumentException("processingDeadlineDays must be at least 1");
        }
        this.processingDeadlineDays = processingDeadlineDays;
    }

    public int getDeadlineApproachingDays() {
        return deadlineApproachingDays;
    }

    public void setDeadlineApproachingDays(int deadlineApproachingDays) {
        if (deadlineApproachingDays < 0) {
            throw new IllegalArgumentException("deadlineApproachingDays must not be negative");
        }
        this.deadlineApproachingDays = deadlineApproachingDays;
    }
}
