package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.ComplaintResponse;
import com.gcivil.complaint.dto.PublicResponseDetailResponse;
import com.gcivil.complaint.exception.ApiException;
import com.gcivil.complaint.repository.ComplaintCategoryRepository;
import com.gcivil.complaint.repository.ComplaintResponseRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicResponseDetailQueryService {

    private final ComplaintResponseRepository complaintResponseRepository;
    private final ComplaintCategoryRepository complaintCategoryRepository;

    public PublicResponseDetailQueryService(
            ComplaintResponseRepository complaintResponseRepository,
            ComplaintCategoryRepository complaintCategoryRepository
    ) {
        this.complaintResponseRepository = complaintResponseRepository;
        this.complaintCategoryRepository = complaintCategoryRepository;
    }

    @Transactional(readOnly = true)
    public PublicResponseDetailResponse getPublicResponseDetail(Long responseId) {
        ComplaintResponse response = complaintResponseRepository.findPublicResponseDetailById(responseId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "공개된 답변을 찾을 수 없습니다."
                ));

        String categoryName = complaintCategoryRepository.findByCategoryCode(
                        response.getComplaint().getCategoryCode().trim().toUpperCase(Locale.ROOT)
                )
                .map(category -> category.getCategoryName())
                .orElse(response.getComplaint().getCategoryCode());

        return new PublicResponseDetailResponse(
                response.getComplaint().getTitle(),
                categoryName,
                response.getComplaint().getSubmittedAt().toLocalDate(),
                response.getComplaint().getAssignedDepartmentName(),
                response.getComplaint().getCompletedAt() == null ? null : response.getComplaint().getCompletedAt().toLocalDate(),
                response.getResponseContent(),
                response.getComplaint().getAttachments().stream()
                        .map(attachment -> new PublicResponseDetailResponse.AttachmentSummary(
                                attachment.getId(),
                                attachment.getOriginalFilename()
                        ))
                        .toList()
        );
    }
}
