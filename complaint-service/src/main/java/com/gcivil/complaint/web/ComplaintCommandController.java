package com.gcivil.complaint.web;

import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.CreateComplaintRequest;
import com.gcivil.complaint.dto.CreateComplaintResponse;
import com.gcivil.complaint.service.ComplaintCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintCommandController {

    private final ComplaintCommandService complaintCommandService;

    public ComplaintCommandController(ComplaintCommandService complaintCommandService) {
        this.complaintCommandService = complaintCommandService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CreateComplaintResponse>> createComplaint(
            @Valid @ModelAttribute CreateComplaintRequest request,
            @RequestHeader("X-User-Id") @Positive(message = "X-User-Id must be positive") Long applicantUserId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        CreateComplaintResponse response = complaintCommandService.createComplaint(request, applicantUserId, requestId);
        String message = response.currentStatus() == ComplaintStatus.ASSIGNED
                ? "민원이 접수되고 담당 부서 및 공무원에게 배정되었습니다."
                : "민원이 접수되었으며 자동 배정 대기 상태로 저장되었습니다.";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, message));
    }
}
