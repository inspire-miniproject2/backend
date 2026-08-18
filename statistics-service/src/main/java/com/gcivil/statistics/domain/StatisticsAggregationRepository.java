package com.gcivil.statistics.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class StatisticsAggregationRepository {
    private final JdbcTemplate jdbcTemplate;

    public StatisticsAggregationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertSource(ComplaintStatisticSource source, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO complaint_statistic_sources
                    (complaint_id, statistic_date, assigned_department_id, category_code, current_status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, source.complaintId(), source.statisticDate(), source.assignedDepartmentId(),
                source.categoryCode(), source.currentStatus(), now);
    }

    public Optional<ComplaintStatisticSource> findSource(Long complaintId) {
        List<ComplaintStatisticSource> results = jdbcTemplate.query("""
                SELECT complaint_id, statistic_date, assigned_department_id, category_code, current_status
                FROM complaint_statistic_sources WHERE complaint_id = ?
                """, (rs, rowNum) -> new ComplaintStatisticSource(
                rs.getLong("complaint_id"), rs.getObject("statistic_date", LocalDate.class),
                rs.getLong("assigned_department_id"), rs.getString("category_code"),
                rs.getString("current_status")), complaintId);
        return results.stream().findFirst();
    }

    public void updateSource(Long complaintId, Long departmentId, String status, LocalDateTime now) {
        jdbcTemplate.update("""
                UPDATE complaint_statistic_sources
                SET assigned_department_id = ?, current_status = ?, updated_at = ?
                WHERE complaint_id = ?
                """, departmentId, status, now, complaintId);
    }

    public void changeCount(LocalDate date, Long departmentId, String categoryCode,
                            String status, long delta, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO complaint_statistics
                    (statistic_date, department_id, category_code, status, complaint_count, updated_at)
                VALUES (?, ?, ?, ?, GREATEST(?, 0), ?)
                ON DUPLICATE KEY UPDATE
                    complaint_count = GREATEST(complaint_count + ?, 0),
                    updated_at = VALUES(updated_at)
                """, date, departmentId, categoryCode, status, delta, now, delta);
    }
}
