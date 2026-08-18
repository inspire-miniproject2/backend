package com.gcivil.complaint.web;

import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.ComplaintDetailResponse;
import com.gcivil.complaint.service.ComplaintDetailQueryService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintDetailQueryController {

    private final ComplaintDetailQueryService complaintDetailQueryService;

    public ComplaintDetailQueryController(ComplaintDetailQueryService complaintDetailQueryService) {
        this.complaintDetailQueryService = complaintDetailQueryService;
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<ApiResponse<ComplaintDetailResponse>> getComplaintDetail(
            @PathVariable @Positive(message = "complaintId must be positive") Long complaintId,
            @RequestHeader("X-User-Id") @Positive(message = "X-User-Id must be positive") Long requesterUserId,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole
    ) {
        ComplaintDetailResponse response =
                complaintDetailQueryService.getComplaintDetail(complaintId, requesterUserId, requesterRole);
        return ResponseEntity.ok(ApiResponse.success(response, "민원 상세를 조회했습니다."));
    }
}
