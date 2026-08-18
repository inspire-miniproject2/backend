package com.gcivil.statistics.application;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StatisticsQueryServiceTest {
    private final StatisticsQueryService service = new StatisticsQueryService(mock(JdbcTemplate.class));

    @Test
    void rejectsReversedDateRange() {
        assertThatThrownBy(() -> service.findDaily(
                LocalDate.parse("2026-08-15"), LocalDate.parse("2026-08-01"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fromDate");
    }
}
