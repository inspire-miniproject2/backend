package com.gcivil.complaint.client;

import com.gcivil.complaint.client.dto.InternalUserResponse;
import com.gcivil.complaint.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "user-service",
        url = "${clients.user-service.url}",
        path = "/api/v1/internal/users"
)
public interface UserServiceClient {

    @GetMapping("/{userId}")
    ApiResponse<InternalUserResponse> getUser(
            @PathVariable("userId") Long userId,
            @RequestHeader("X-Internal-Caller") String internalCaller,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    );
}
