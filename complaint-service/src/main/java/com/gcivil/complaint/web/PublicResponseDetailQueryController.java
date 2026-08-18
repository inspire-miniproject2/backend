package com.gcivil.complaint.web;

import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.PublicResponseDetailResponse;
import com.gcivil.complaint.service.PublicResponseDetailQueryService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public-responses")
public class PublicResponseDetailQueryController {

    private final PublicResponseDetailQueryService publicResponseDetailQueryService;

    public PublicResponseDetailQueryController(PublicResponseDetailQueryService publicResponseDetailQueryService) {
        this.publicResponseDetailQueryService = publicResponseDetailQueryService;
    }

    @GetMapping("/{responseId}")
    public ResponseEntity<ApiResponse<PublicResponseDetailResponse>> getPublicResponseDetail(
            @PathVariable @Positive(message = "responseId must be positive") Long responseId
    ) {
        PublicResponseDetailResponse response = publicResponseDetailQueryService.getPublicResponseDetail(responseId);
        return ResponseEntity.ok(ApiResponse.success(response, "공개 답변 상세를 조회했습니다."));
    }
}
