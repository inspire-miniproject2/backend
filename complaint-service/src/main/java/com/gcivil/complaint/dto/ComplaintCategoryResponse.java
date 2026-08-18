package com.gcivil.complaint.dto;

public record ComplaintCategoryResponse(
        Long categoryId,
        String categoryName,
        String categoryCode
) {
}
