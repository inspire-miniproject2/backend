package com.gcivil.statistics.web;

import com.gcivil.statistics.application.DailyStatisticsResponse;
import com.gcivil.statistics.application.StatisticsQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsControllerTest {
    private final StatisticsQueryService queryService = mock(StatisticsQueryService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StatisticsController(queryService))
                .setControllerAdvice(new StatisticsExceptionHandler())
                .build();
    }

    @Test
    void returnsDailyStatisticsUsingDateAndDepartmentFilters() throws Exception {
        when(queryService.findDaily(LocalDate.parse("2026-08-01"),
                LocalDate.parse("2026-08-14"), 21L))
                .thenReturn(new DailyStatisticsResponse(List.of()));

        mockMvc.perform(get("/api/v1/admin/statistics/daily")
                        .param("fromDate", "2026-08-01")
                        .param("toDate", "2026-08-14")
                        .param("departmentId", "21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.message").value("일별 통계를 조회했습니다."));
    }
}
