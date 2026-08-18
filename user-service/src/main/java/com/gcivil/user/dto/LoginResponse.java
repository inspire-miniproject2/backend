package com.gcivil.user.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserSummary user
) {
    public record UserSummary(
            Long userId,
            String loginId,
            String role,
            Long departmentId
    ) {
    }
}
