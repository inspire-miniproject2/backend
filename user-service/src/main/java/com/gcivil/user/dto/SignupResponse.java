package com.gcivil.user.dto;

import java.time.LocalDateTime;

public record SignupResponse(
        Long userId,
        String loginId,
        String role,
        LocalDateTime createdAt
) {
}
