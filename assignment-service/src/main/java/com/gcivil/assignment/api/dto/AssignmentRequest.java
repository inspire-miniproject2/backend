package com.gcivil.assignment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;

public record AssignmentRequest(
        @NotNull @Positive Long complaintId,
        @NotBlank String complaintNo,
        @NotNull @Positive Long categoryId,
        @NotBlank String categoryCode,
        @NotNull @Positive Long applicantUserId,
        @NotNull LocalDateTime submittedAt
) {
}
