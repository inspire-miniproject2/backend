package com.gcivil.statistics.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsQueryService {
    private final JdbcTemplate jdbcTemplate;

    public StatisticsQueryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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

        List<DailyStatisticResponse> content = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            long completed = rs.getLong("completed_count");
            return new DailyStatisticResponse(
                    rs.getObject("statistic_date", LocalDate.class), rs.getLong("total_count"), completed,
                    rs.getLong("received_count"), rs.getLong("assigned_count"),
                    rs.getLong("in_progress_count"), completed, 0, 0, BigDecimal.ZERO);
        }, arguments.toArray());
        return new DailyStatisticsResponse(content);
    }
}
