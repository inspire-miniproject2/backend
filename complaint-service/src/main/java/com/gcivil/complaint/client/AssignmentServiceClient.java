package com.gcivil.complaint.client;

import com.gcivil.complaint.client.dto.AssignmentRequest;
import com.gcivil.complaint.client.dto.AssignmentResponse;
import com.gcivil.complaint.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "assignment-service",
        url = "${clients.assignment-service.url}",
        path = "/api/v1/internal/assignments"
)
public interface AssignmentServiceClient {

    @PostMapping
    ApiResponse<AssignmentResponse> assign(
            @RequestBody AssignmentRequest request,
            @RequestHeader("X-Internal-Caller") String internalCaller,
            @RequestHeader("X-Request-Id") String requestId
    );
}
