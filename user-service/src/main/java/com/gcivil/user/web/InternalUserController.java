package com.gcivil.user.web;

import com.gcivil.user.dto.ApiResponse;
import com.gcivil.user.dto.InternalUserResponse;
import com.gcivil.user.service.InternalUserLookupService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final InternalUserLookupService internalUserLookupService;

    public InternalUserController(InternalUserLookupService internalUserLookupService) {
        this.internalUserLookupService = internalUserLookupService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<InternalUserResponse>> getUser(
            @PathVariable @Positive(message = "userId must be positive") Long userId
    ) {
        InternalUserResponse response = internalUserLookupService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.success(response, "내부 사용자 정보를 조회했습니다."));
    }
}
