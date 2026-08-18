package com.gcivil.statistics.web;

import com.gcivil.statistics.application.DailyStatisticsResponse;
import com.gcivil.statistics.application.StatisticsQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/statistics")
public class StatisticsController {
    private final StatisticsQueryService queryService;

    public StatisticsController(StatisticsQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/daily")
    public ApiResponse<DailyStatisticsResponse> findDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long departmentId
    ) {
        return ApiResponse.success(queryService.findDaily(fromDate, toDate, departmentId),
                "일별 통계를 조회했습니다.");
    }
}
