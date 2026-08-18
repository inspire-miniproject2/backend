package com.gcivil.user.dto;

public record InternalUserResponse(
        Long userId,
        String loginId,
        String name,
        String role,
        Long departmentId,
        boolean isActive
) {
}
