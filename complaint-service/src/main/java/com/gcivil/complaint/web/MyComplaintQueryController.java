package com.gcivil.complaint.web;

import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.MyComplaintListResponse;
import com.gcivil.complaint.service.MyComplaintQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/complaints")
public class MyComplaintQueryController {

    private final MyComplaintQueryService myComplaintQueryService;

    public MyComplaintQueryController(MyComplaintQueryService myComplaintQueryService) {
        this.myComplaintQueryService = myComplaintQueryService;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<MyComplaintListResponse>> getMyComplaints(
            @RequestHeader("X-User-Id") @Positive(message = "X-User-Id must be positive") Long applicantUserId,
            @RequestHeader("X-User-Role") String requesterRole,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be greater than or equal to 0") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be less than or equal to 100") Integer size,
            @RequestParam(required = false) @Size(max = 50, message = "keyword must be at most 50 characters") String keyword,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) String status
    ) {
        MyComplaintListResponse response =
                myComplaintQueryService.getMyComplaints(
                        applicantUserId,
                        requesterRole,
                        page,
                        size,
                        keyword,
                        categoryCode,
                        status
                );
        return ResponseEntity.ok(ApiResponse.success(response, "내 민원 목록을 조회했습니다."));
    }
}
