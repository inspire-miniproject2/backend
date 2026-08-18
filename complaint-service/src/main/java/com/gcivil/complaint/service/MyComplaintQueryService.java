package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.dto.MyComplaintListResponse;
import com.gcivil.complaint.exception.ApiException;
import com.gcivil.complaint.repository.ComplaintRepository;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MyComplaintQueryService {

    private final ComplaintRepository complaintRepository;
    private final UserAccessGuard userAccessGuard;

    public MyComplaintQueryService(
            ComplaintRepository complaintRepository,
            UserAccessGuard userAccessGuard
    ) {
        this.complaintRepository = complaintRepository;
        this.userAccessGuard = userAccessGuard;
    }

    @Transactional(readOnly = true)
    public MyComplaintListResponse getMyComplaints(
            Long applicantUserId,
            String requesterRole,
            Integer page,
            Integer size,
            String keyword,
            String categoryCode,
            String status
    ) {
        userAccessGuard.requireCitizen(requesterRole);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        ComplaintStatus complaintStatus = parseStatus(status);
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedCategoryCode = normalizeCategoryCode(categoryCode);

        Page<Complaint> complaints = complaintRepository.searchMyComplaints(
                applicantUserId,
                normalizedCategoryCode,
                complaintStatus,
                normalizedKeyword,
                pageable
        );

        return new MyComplaintListResponse(
                new MyComplaintListResponse.Summary(
                        complaintRepository.countByApplicantUserId(applicantUserId),
                        complaintRepository.countByApplicantUserIdAndCurrentStatus(applicantUserId, ComplaintStatus.RECEIVED),
                        complaintRepository.countByApplicantUserIdAndCurrentStatus(applicantUserId, ComplaintStatus.ASSIGNED),
                        complaintRepository.countByApplicantUserIdAndCurrentStatus(applicantUserId, ComplaintStatus.IN_PROGRESS),
                        complaintRepository.countByApplicantUserIdAndCurrentStatus(applicantUserId, ComplaintStatus.COMPLETED)
                ),
                complaints.getContent().stream()
                        .map(this::toSummary)
                        .toList(),
                complaints.getNumber(),
                complaints.getSize(),
                complaints.getTotalElements(),
                complaints.getTotalPages(),
                complaints.hasNext()
        );
    }

    private MyComplaintListResponse.MyComplaintSummary toSummary(Complaint complaint) {
        return new MyComplaintListResponse.MyComplaintSummary(
                complaint.getId(),
                complaint.getComplaintNo(),
                complaint.getTitle(),
                complaint.getCategoryCode(),
                complaint.getCurrentStatus().name(),
                complaint.getAssignedDepartmentName(),
                complaint.getAssignedDepartmentId(),
                complaint.getAssignedOfficerUserId()
        );
    }

    private ComplaintStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return ComplaintStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "status 값이 올바르지 않습니다."
            );
        }
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
