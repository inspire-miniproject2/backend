package com.gcivil.complaint.service;

import com.gcivil.complaint.client.UserServiceClient;
import com.gcivil.complaint.client.dto.InternalUserResponse;
import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.dto.AssignedComplaintListResponse;
import com.gcivil.complaint.exception.ApiException;
import com.gcivil.complaint.repository.ComplaintRepository;
import feign.FeignException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintOfficerQueryService {

    private final ComplaintRepository complaintRepository;
    private final UserServiceClient userServiceClient;
    private final UserAccessGuard userAccessGuard;

    public ComplaintOfficerQueryService(
            ComplaintRepository complaintRepository,
            UserServiceClient userServiceClient,
            UserAccessGuard userAccessGuard
    ) {
        this.complaintRepository = complaintRepository;
        this.userServiceClient = userServiceClient;
        this.userAccessGuard = userAccessGuard;
    }

    @Transactional(readOnly = true)
    public AssignedComplaintListResponse getAssignedComplaints(
            Long officerUserId,
            String requesterRole,
            Integer page,
            Integer size,
            String status,
            String keyword
    ) {
        return getAssignedComplaints(officerUserId, requesterRole, page, size, status, keyword, null);
    }

    @Transactional(readOnly = true)
    public AssignedComplaintListResponse getAssignedComplaints(
            Long officerUserId,
            String requesterRole,
            Integer page,
            Integer size,
            String status,
            String keyword,
            String requestId
    ) {
        userAccessGuard.requireOfficerOrAdmin(requesterRole);
        boolean admin = "ADMIN".equals(userAccessGuard.normalizeRole(requesterRole));
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        ComplaintStatus complaintStatus = parseStatus(status);
        String normalizedKeyword = normalizeKeyword(keyword);

        Long assignedOfficerUserId = admin ? null : officerUserId;
        Page<Complaint> complaints = complaintRepository.searchOfficerComplaints(
                assignedOfficerUserId,
                complaintStatus,
                normalizedKeyword,
                pageable
        );
        Map<Long, String> assigneeNames = fetchAssigneeNames(complaints, requestId);

        return new AssignedComplaintListResponse(
                new AssignedComplaintListResponse.Summary(
                        countByStatus(officerUserId, ComplaintStatus.ASSIGNED, admin),
                        countByStatus(officerUserId, ComplaintStatus.IN_PROGRESS, admin),
                        countByStatus(officerUserId, ComplaintStatus.COMPLETED, admin)
                ),
                complaints.getContent().stream()
                        .map(complaint -> toSummary(complaint, assigneeNames))
                        .toList(),
                complaints.getNumber(),
                complaints.getSize(),
                complaints.getTotalElements(),
                complaints.getTotalPages(),
                complaints.hasNext()
        );
    }

    private long countByStatus(Long officerUserId, ComplaintStatus status, boolean admin) {
        return admin
                ? complaintRepository.countByCurrentStatus(status)
                : complaintRepository.countByAssignedOfficerUserIdAndCurrentStatus(officerUserId, status);
    }

    private Map<Long, String> fetchAssigneeNames(Page<Complaint> complaints, String requestId) {
        Set<Long> officerIds = complaints.getContent().stream()
                .map(Complaint::getAssignedOfficerUserId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, String> assigneeNames = new HashMap<>();
        for (Long officerId : officerIds) {
            InternalUserResponse user = fetchOfficer(officerId, requestId);
            assigneeNames.put(officerId, user == null ? null : user.name());
        }
        return assigneeNames;
    }

    private AssignedComplaintListResponse.AssignedComplaintSummary toSummary(
            Complaint complaint,
            Map<Long, String> assigneeNames
    ) {
        return new AssignedComplaintListResponse.AssignedComplaintSummary(
                complaint.getId(),
                complaint.getComplaintNo(),
                complaint.getTitle(),
                assigneeNames.get(complaint.getAssignedOfficerUserId()),
                complaint.getAssignedOfficerUserId(),
                complaint.getCurrentStatus().name(),
                complaint.getAssignedDepartmentId()
        );
    }

    private InternalUserResponse fetchOfficer(Long officerUserId, String requestId) {
        try {
            ApiResponse<InternalUserResponse> response = userServiceClient.getUser(
                    officerUserId,
                    "complaint-service",
                    normalizeRequestId(requestId)
            );
            if (response == null || !response.success() || response.data() == null) {
                throw new ApiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SERVICE_UNAVAILABLE",
                        "user-service 응답이 올바르지 않습니다."
                );
            }
            return response.data();
        } catch (FeignException.NotFound ex) {
            return null;
        } catch (FeignException ex) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SERVICE_UNAVAILABLE",
                    "user-service 호출에 실패했습니다."
            );
        }
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

    private String normalizeRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }
}
