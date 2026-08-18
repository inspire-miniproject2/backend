package com.gcivil.assignment.client;

import com.gcivil.assignment.client.dto.ApiSuccessResponse;
import com.gcivil.assignment.client.dto.InternalUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "user-service", path = "/api/v1/internal/users")
public interface UserServiceClient {

    @GetMapping("/{userId}")
    ApiSuccessResponse<InternalUserResponse> getUser(
            @PathVariable("userId") Long userId,
            @RequestHeader("X-Internal-Caller") String internalCaller,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    );
}
