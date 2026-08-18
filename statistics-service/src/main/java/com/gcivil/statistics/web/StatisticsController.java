package com.gcivil.statistics.web;

import com.gcivil.statistics.application.DailyStatisticsResponse;
import com.gcivil.statistics.application.StatisticsQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long departmentId
    ) {
        if (role == null) {
            throw new StatisticsAuthorizationException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }
        if (!"ADMIN".equals(role)) {
            throw new StatisticsAuthorizationException(HttpStatus.FORBIDDEN, "Admin role is required");
        }
        return ApiResponse.success(queryService.findDaily(fromDate, toDate, departmentId),
                "일별 통계를 조회했습니다.");
    }
}
