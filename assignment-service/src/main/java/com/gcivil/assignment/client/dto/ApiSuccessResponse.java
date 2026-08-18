package com.gcivil.assignment.client.dto;

public record ApiSuccessResponse<T>(
        boolean success,
        T data,
        String message
) {
}
