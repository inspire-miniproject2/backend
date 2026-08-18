package com.gcivil.complaint.client.dto;

public record InternalUserResponse(
        Long userId,
        String loginId,
        String name,
        String role,
        Long departmentId,
        boolean isActive
) {
}
