package com.gcivil.complaint.web;

import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.ComplaintCategoryResponse;
import com.gcivil.complaint.service.ComplaintCategoryQueryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/complaint-categories")
public class ComplaintCategoryQueryController {

    private final ComplaintCategoryQueryService complaintCategoryQueryService;

    public ComplaintCategoryQueryController(ComplaintCategoryQueryService complaintCategoryQueryService) {
        this.complaintCategoryQueryService = complaintCategoryQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ComplaintCategoryResponse>>> getCategories(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        List<ComplaintCategoryResponse> response = complaintCategoryQueryService.getCategories(activeOnly);
        return ResponseEntity.ok(ApiResponse.success(response, "카테고리 목록을 조회했습니다."));
    }
}
