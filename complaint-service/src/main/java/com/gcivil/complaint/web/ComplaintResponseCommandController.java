package com.gcivil.complaint.web;

import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.RegisterComplaintResponseRequest;
import com.gcivil.complaint.dto.RegisterComplaintResponseResponse;
import com.gcivil.complaint.service.ComplaintResponseCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/officer/complaints")
public class ComplaintResponseCommandController {

    private final ComplaintResponseCommandService complaintResponseCommandService;

    public ComplaintResponseCommandController(ComplaintResponseCommandService complaintResponseCommandService) {
        this.complaintResponseCommandService = complaintResponseCommandService;
    }

    @PostMapping("/{complaintId}/response")
    public ResponseEntity<ApiResponse<RegisterComplaintResponseResponse>> registerResponse(
            @PathVariable @Positive(message = "complaintId must be positive") Long complaintId,
            @Valid @RequestBody RegisterComplaintResponseRequest request,
            @RequestHeader("X-User-Id") @Positive(message = "X-User-Id must be positive") Long responderUserId
    ) {
        RegisterComplaintResponseResponse response =
                complaintResponseCommandService.registerResponse(complaintId, request, responderUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "민원 답변이 등록되었습니다."));
    }
}
