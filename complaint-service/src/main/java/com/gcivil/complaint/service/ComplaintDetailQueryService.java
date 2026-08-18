package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintAttachment;
import com.gcivil.complaint.domain.ComplaintResponse;
import com.gcivil.complaint.domain.ComplaintStatusHistory;
import com.gcivil.complaint.dto.ComplaintDetailResponse;
import com.gcivil.complaint.exception.ApiException;
import com.gcivil.complaint.repository.ComplaintRepository;
import com.gcivil.complaint.repository.ComplaintResponseRepository;
import com.gcivil.complaint.repository.ComplaintStatusHistoryRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintDetailQueryService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintResponseRepository complaintResponseRepository;
    private final ComplaintStatusHistoryRepository complaintStatusHistoryRepository;

    public ComplaintDetailQueryService(
            ComplaintRepository complaintRepository,
            ComplaintResponseRepository complaintResponseRepository,
            ComplaintStatusHistoryRepository complaintStatusHistoryRepository
    ) {
        this.complaintRepository = complaintRepository;
        this.complaintResponseRepository = complaintResponseRepository;
        this.complaintStatusHistoryRepository = complaintStatusHistoryRepository;
    }

    @Transactional(readOnly = true)
    public ComplaintDetailResponse getComplaintDetail(Long complaintId, Long requesterUserId, String requesterRole) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "민원을 찾을 수 없습니다."
                ));

        validateAccess(complaint, requesterUserId, requesterRole);

        ComplaintResponse response = complaintResponseRepository.findByComplaint_Id(complaintId).orElse(null);
        List<ComplaintStatusHistory> statusHistories =
                complaintStatusHistoryRepository.findByComplaint_IdOrderByChangedAtAsc(complaintId);

        return new ComplaintDetailResponse(
                complaint.getId(),
                complaint.getComplaintNo(),
                complaint.getApplicantUserId(),
                complaint.getCategoryCode(),
                complaint.getTitle(),
                complaint.getContent(),
                complaint.getCurrentStatus().name(),
                complaint.getAssignedDepartmentName(),
                complaint.getAssignedDepartmentId(),
                complaint.getAssignedOfficerUserId(),
                complaint.getSubmittedAt(),
                complaint.getAssignedAt(),
                complaint.getCompletedAt(),
                toResponseSummary(response),
                complaint.getAttachments().stream().map(this::toAttachmentSummary).toList(),
                statusHistories.stream().map(this::toStatusHistorySummary).toList()
        );
    }

    private void validateAccess(Complaint complaint, Long requesterUserId, String requesterRole) {
        String normalizedRole = normalizeRole(requesterRole);
        if ("OFFICER".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return;
        }
        if (!complaint.getApplicantUserId().equals(requesterUserId)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "해당 민원에 접근할 권한이 없습니다."
            );
        }
    }

    private String normalizeRole(String requesterRole) {
        if (requesterRole == null || requesterRole.isBlank()) {
            return "CITIZEN";
        }
        return requesterRole.trim().toUpperCase(Locale.ROOT);
    }

    private ComplaintDetailResponse.ResponseSummary toResponseSummary(ComplaintResponse response) {
        if (response == null) {
            return null;
        }
        return new ComplaintDetailResponse.ResponseSummary(
                response.getId(),
                response.getResponderUserId(),
                response.isPublic(),
                response.getResponseContent(),
                response.getRespondedAt()
        );
    }

    private ComplaintDetailResponse.AttachmentSummary toAttachmentSummary(ComplaintAttachment attachment) {
        return new ComplaintDetailResponse.AttachmentSummary(
                attachment.getId(),
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getFileSize(),
                attachment.getUploadedAt()
        );
    }

    private ComplaintDetailResponse.StatusHistorySummary toStatusHistorySummary(ComplaintStatusHistory history) {
        return new ComplaintDetailResponse.StatusHistorySummary(
                history.getPreviousStatus() == null ? null : history.getPreviousStatus().name(),
                history.getNewStatus().name(),
                history.getChangedByUserId(),
                history.getChangeMemo(),
                history.getChangedAt()
        );
    }
}
