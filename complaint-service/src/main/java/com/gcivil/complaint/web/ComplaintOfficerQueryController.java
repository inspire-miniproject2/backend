package com.gcivil.complaint.web;

import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.AssignedComplaintListResponse;
import com.gcivil.complaint.service.ComplaintOfficerQueryService;
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
@RequestMapping("/api/v1/officer/complaints")
public class ComplaintOfficerQueryController {

    private final ComplaintOfficerQueryService complaintOfficerQueryService;

    public ComplaintOfficerQueryController(ComplaintOfficerQueryService complaintOfficerQueryService) {
        this.complaintOfficerQueryService = complaintOfficerQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AssignedComplaintListResponse>> getAssignedComplaints(
            @RequestHeader("X-User-Id") @Positive(message = "X-User-Id must be positive") Long officerUserId,
            @RequestHeader("X-User-Role") String requesterRole,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be greater than or equal to 0") Integer page,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be less than or equal to 100") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @Size(max = 50, message = "keyword must be at most 50 characters") String keyword
    ) {
        AssignedComplaintListResponse response =
                complaintOfficerQueryService.getAssignedComplaints(
                        officerUserId,
                        requesterRole,
                        page,
                        size,
                        status,
                        keyword,
                        requestId
                );
        return ResponseEntity.ok(ApiResponse.success(response, "담당 민원 목록을 조회했습니다."));
    }
}
