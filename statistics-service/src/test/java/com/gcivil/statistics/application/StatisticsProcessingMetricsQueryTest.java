package com.gcivil.statistics.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcivil.statistics.config.StatisticsMetricsProperties;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

@JdbcTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:metrics;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false"
})
class StatisticsProcessingMetricsQueryTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private StatisticsQueryService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS complaint_statistic_sources");
        jdbcTemplate.execute("DROP TABLE IF EXISTS complaint_statistics");
        jdbcTemplate.execute("""
                CREATE TABLE complaint_statistics (
                    statistic_date DATE NOT NULL, department_id BIGINT NOT NULL,
                    category_code VARCHAR(50) NOT NULL, status VARCHAR(30) NOT NULL,
                    complaint_count BIGINT NOT NULL, updated_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (statistic_date, department_id, category_code, status))
                """);
        jdbcTemplate.execute("""
                CREATE TABLE complaint_statistic_sources (
                    complaint_id BIGINT PRIMARY KEY, statistic_date DATE NOT NULL,
                    assigned_department_id BIGINT NOT NULL, category_code VARCHAR(50) NOT NULL,
                    current_status VARCHAR(30) NOT NULL, submitted_at TIMESTAMP,
                    due_at TIMESTAMP, completed_at TIMESTAMP, updated_at TIMESTAMP NOT NULL)
                """);
        jdbcTemplate.update("""
                INSERT INTO complaint_statistics VALUES
                ('2026-08-15', 10, 'TRAFFIC', 'COMPLETED', 1, CURRENT_TIMESTAMP),
                ('2026-08-15', 10, 'TRAFFIC', 'IN_PROGRESS', 2, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO complaint_statistic_sources VALUES
                (1, '2026-08-15', 10, 'TRAFFIC', 'COMPLETED',
                 '2026-08-15 09:00:00', '2026-08-22 09:00:00', '2026-08-15 11:00:00', CURRENT_TIMESTAMP),
                (2, '2026-08-15', 10, 'TRAFFIC', 'IN_PROGRESS',
                 '2026-08-12 09:00:00', '2026-08-20 09:00:00', NULL, CURRENT_TIMESTAMP),
                (3, '2026-08-15', 10, 'TRAFFIC', 'ASSIGNED',
                 '2026-08-10 09:00:00', '2026-08-18 09:00:00', NULL, CURRENT_TIMESTAMP)
                """);
        service = new StatisticsQueryService(jdbcTemplate,
                Clock.fixed(Instant.parse("2026-08-19T00:00:00Z"), ZoneOffset.UTC),
                new StatisticsMetricsProperties());
    }

    @Test
    void returnsAverageProcessingHoursAndDeadlineCounts() {
        DailyStatisticResponse result = service.findDaily(
                LocalDate.parse("2026-08-15"), LocalDate.parse("2026-08-15"), 10L)
                .content().getFirst();

        assertThat(result.deadlineApproachingCount()).isEqualTo(1);
        assertThat(result.overdueCount()).isEqualTo(1);
        assertThat(result.averageProcessingHours()).isEqualByComparingTo(new BigDecimal("2.00"));
    }
}
