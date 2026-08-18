package com.gcivil.complaint.web;

import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.PublicResponseListResponse;
import com.gcivil.complaint.service.PublicResponseQueryService;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/public-responses")
public class PublicResponseQueryController {

    private final PublicResponseQueryService publicResponseQueryService;

    public PublicResponseQueryController(PublicResponseQueryService publicResponseQueryService) {
        this.publicResponseQueryService = publicResponseQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PublicResponseListResponse>> getPublicResponses(
            @RequestParam(required = false) @Size(max = 50, message = "keyword must be at most 50 characters") String keyword,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedTo
    ) {
        PublicResponseListResponse response =
                publicResponseQueryService.getPublicResponses(keyword, categoryCode, completedFrom, completedTo);
        return ResponseEntity.ok(ApiResponse.success(response, "공개 답변 목록을 조회했습니다."));
    }
}
