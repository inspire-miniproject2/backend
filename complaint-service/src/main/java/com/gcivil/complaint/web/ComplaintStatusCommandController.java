package com.gcivil.complaint.web;

import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.ChangeComplaintStatusRequest;
import com.gcivil.complaint.dto.ChangeComplaintStatusResponse;
import com.gcivil.complaint.service.ComplaintStatusCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/officer/complaints")
public class ComplaintStatusCommandController {

    private final ComplaintStatusCommandService complaintStatusCommandService;

    public ComplaintStatusCommandController(ComplaintStatusCommandService complaintStatusCommandService) {
        this.complaintStatusCommandService = complaintStatusCommandService;
    }

    @PatchMapping("/{complaintId}/status")
    public ResponseEntity<ApiResponse<ChangeComplaintStatusResponse>> changeStatus(
            @PathVariable @Positive(message = "complaintId must be positive") Long complaintId,
            @Valid @RequestBody ChangeComplaintStatusRequest request,
            @RequestHeader("X-User-Id") @Positive(message = "X-User-Id must be positive") Long changedByUserId
    ) {
        ChangeComplaintStatusResponse response =
                complaintStatusCommandService.changeStatus(complaintId, request, changedByUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "민원 상태가 변경되었습니다."));
    }
}
