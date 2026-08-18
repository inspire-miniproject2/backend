package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.ComplaintResponse;
import com.gcivil.complaint.dto.PublicResponseListResponse;
import com.gcivil.complaint.repository.ComplaintResponseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicResponseQueryService {

    private static final String STATUS_LABEL = "답변 완료";

    private final ComplaintResponseRepository complaintResponseRepository;

    public PublicResponseQueryService(ComplaintResponseRepository complaintResponseRepository) {
        this.complaintResponseRepository = complaintResponseRepository;
    }

    @Transactional(readOnly = true)
    public PublicResponseListResponse getPublicResponses(
            String keyword,
            String categoryCode,
            LocalDate completedFrom,
            LocalDate completedTo
    ) {
        List<ComplaintResponse> responses = complaintResponseRepository.searchPublicResponses(
                normalizeKeyword(keyword),
                normalizeCategoryCode(categoryCode),
                completedFrom == null ? null : completedFrom.atStartOfDay(),
                completedTo == null ? null : completedTo.plusDays(1).atStartOfDay()
        );

        return new PublicResponseListResponse(
                responses.stream()
                        .map(this::toSummary)
                        .toList()
        );
    }

    private PublicResponseListResponse.PublicResponseSummary toSummary(ComplaintResponse response) {
        return new PublicResponseListResponse.PublicResponseSummary(
                response.getId(),
                response.getComplaint().getTitle(),
                response.getComplaint().getAssignedDepartmentName(),
                response.getComplaint().getCompletedAt() == null ? null : response.getComplaint().getCompletedAt().toLocalDate(),
                STATUS_LABEL
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    private String normalizeCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return null;
        }
        return categoryCode.trim().toUpperCase(Locale.ROOT);
    }
}
