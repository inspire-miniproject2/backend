package com.gcivil.assignment.web;

import com.gcivil.assignment.api.dto.ApiResponse;
import com.gcivil.assignment.api.dto.AssignmentRequest;
import com.gcivil.assignment.api.dto.AssignmentResponse;
import com.gcivil.assignment.service.AssignmentDecisionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/assignments")
public class InternalAssignmentController {

    private final AssignmentDecisionService assignmentDecisionService;

    public InternalAssignmentController(AssignmentDecisionService assignmentDecisionService) {
        this.assignmentDecisionService = assignmentDecisionService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssignmentResponse>> assign(
            @Valid @RequestBody AssignmentRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        AssignmentResponse response = assignmentDecisionService.assign(request, requestId);
        String message = response.assignmentFound()
                ? "담당 부서와 담당 공무원을 배정했습니다."
                : "자동 배정 대상을 찾지 못했습니다.";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }
}
