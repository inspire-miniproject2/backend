package com.gcivil.statistics.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gcivil.statistics.config.StatisticsMetricsProperties;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsQueryService {
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final StatisticsMetricsProperties metricsProperties;

    public StatisticsQueryService(JdbcTemplate jdbcTemplate, Clock clock,
                                  StatisticsMetricsProperties metricsProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.metricsProperties = metricsProperties;
    }

    @Transactional(readOnly = true)
    public DailyStatisticsResponse findDaily(LocalDate fromDate, LocalDate toDate, Long departmentId) {
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must not be after toDate");
        }

        StringBuilder sql = new StringBuilder("""
                SELECT statistic_date,
                       SUM(complaint_count) AS total_count,
                       SUM(CASE WHEN status = 'RECEIVED' THEN complaint_count ELSE 0 END) AS received_count,
                       SUM(CASE WHEN status = 'ASSIGNED' THEN complaint_count ELSE 0 END) AS assigned_count,
                       SUM(CASE WHEN status = 'IN_PROGRESS' THEN complaint_count ELSE 0 END) AS in_progress_count,
                       SUM(CASE WHEN status = 'COMPLETED' THEN complaint_count ELSE 0 END) AS completed_count
                FROM complaint_statistics
                WHERE statistic_date BETWEEN ? AND ?
                """);
        List<Object> arguments = new ArrayList<>(List.of(fromDate, toDate));
        if (departmentId != null) {
            sql.append(" AND department_id = ?");
            arguments.add(departmentId);
        }
        sql.append(" GROUP BY statistic_date ORDER BY statistic_date");

        Map<LocalDate, ProcessingMetrics> metricsByDate = findProcessingMetrics(
                fromDate, toDate, departmentId);
        List<DailyStatisticResponse> content = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            long completed = rs.getLong("completed_count");
            LocalDate statisticDate = rs.getObject("statistic_date", LocalDate.class);
            ProcessingMetrics metrics = metricsByDate.getOrDefault(statisticDate, ProcessingMetrics.EMPTY);
            return new DailyStatisticResponse(
                    statisticDate, rs.getLong("total_count"), completed,
                    rs.getLong("received_count"), rs.getLong("assigned_count"),
                    rs.getLong("in_progress_count"), completed,
                    metrics.deadlineApproachingCount(), metrics.overdueCount(),
                    metrics.averageProcessingHours());
        }, arguments.toArray());
        return new DailyStatisticsResponse(content);
    }

    private Map<LocalDate, ProcessingMetrics> findProcessingMetrics(
            LocalDate fromDate, LocalDate toDate, Long departmentId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime approachingUntil = now.plusDays(metricsProperties.getDeadlineApproachingDays());
        StringBuilder sql = new StringBuilder("""
                SELECT statistic_date,
                       SUM(CASE WHEN current_status <> 'COMPLETED'
                                     AND due_at >= ? AND due_at <= ? THEN 1 ELSE 0 END)
                           AS deadline_approaching_count,
                       SUM(CASE WHEN current_status <> 'COMPLETED'
                                     AND due_at < ? THEN 1 ELSE 0 END) AS overdue_count,
                       AVG(CASE WHEN completed_at IS NOT NULL AND submitted_at IS NOT NULL
                                THEN TIMESTAMPDIFF(MINUTE, submitted_at, completed_at) END)
                           AS average_processing_minutes
                FROM complaint_statistic_sources
                WHERE statistic_date BETWEEN ? AND ?
                """);
        List<Object> arguments = new ArrayList<>(List.of(now, approachingUntil, now, fromDate, toDate));
        if (departmentId != null) {
            sql.append(" AND assigned_department_id = ?");
            arguments.add(departmentId);
        }
        sql.append(" GROUP BY statistic_date ORDER BY statistic_date");

        Map<LocalDate, ProcessingMetrics> result = new LinkedHashMap<>();
        jdbcTemplate.query(sql.toString(), rs -> {
            BigDecimal averageMinutes = rs.getBigDecimal("average_processing_minutes");
            BigDecimal averageHours = averageMinutes == null ? BigDecimal.ZERO
                    : averageMinutes.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
            result.put(rs.getObject("statistic_date", LocalDate.class), new ProcessingMetrics(
                    rs.getLong("deadline_approaching_count"), rs.getLong("overdue_count"), averageHours));
        }, arguments.toArray());
        return result;
    }

    private record ProcessingMetrics(
            long deadlineApproachingCount,
            long overdueCount,
            BigDecimal averageProcessingHours
    ) {
        private static final ProcessingMetrics EMPTY = new ProcessingMetrics(0, 0, BigDecimal.ZERO);
    }
}
