package com.gcivil.user.dto;

public record ApiErrorResponse(
        boolean success,
        ErrorBody error,
        String requestId
) {
    public static ApiErrorResponse of(String code, String message, Object details, String requestId) {
        return new ApiErrorResponse(false, new ErrorBody(code, message, details), requestId);
    }

    public record ErrorBody(
            String code,
            String message,
            Object details
    ) {
    }
}
