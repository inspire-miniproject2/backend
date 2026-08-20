package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintResponse;
import com.gcivil.complaint.dto.RegisterComplaintResponseRequest;
import com.gcivil.complaint.dto.RegisterComplaintResponseResponse;
import com.gcivil.complaint.event.ComplaintEventPublisher;
import com.gcivil.complaint.event.ComplaintResponseRegisteredPayload;
import com.gcivil.complaint.event.NotifyChannel;
import com.gcivil.complaint.exception.ApiException;
import com.gcivil.complaint.repository.ComplaintRepository;
import com.gcivil.complaint.repository.ComplaintResponseRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintResponseCommandService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintResponseRepository complaintResponseRepository;
    private final ComplaintStatusPolicy complaintStatusPolicy;
    private final ComplaintEventPublisher complaintEventPublisher;
    private final UserAccessGuard userAccessGuard;

    public ComplaintResponseCommandService(
            ComplaintRepository complaintRepository,
            ComplaintResponseRepository complaintResponseRepository,
            ComplaintStatusPolicy complaintStatusPolicy,
            ComplaintEventPublisher complaintEventPublisher,
            UserAccessGuard userAccessGuard
    ) {
        this.complaintRepository = complaintRepository;
        this.complaintResponseRepository = complaintResponseRepository;
        this.complaintStatusPolicy = complaintStatusPolicy;
        this.complaintEventPublisher = complaintEventPublisher;
        this.userAccessGuard = userAccessGuard;
    }

    @Transactional
    public RegisterComplaintResponseResponse registerResponse(
            Long complaintId,
            RegisterComplaintResponseRequest request,
            Long responderUserId,
            String requesterRole,
            Long departmentId
    ) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "민원을 찾을 수 없습니다."
                ));
        userAccessGuard.requireAssignedDepartmentOrAdmin(complaint, requesterRole, departmentId);

        complaintStatusPolicy.validateResponseRegistrationAllowed(complaint.getCurrentStatus());

        if (complaintResponseRepository.existsByComplaint_Id(complaintId)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DUPLICATE_RESOURCE",
                    "해당 민원에는 이미 공식 답변이 등록되어 있습니다."
            );
        }

        LocalDateTime now = LocalDateTime.now();
        ComplaintResponse complaintResponse = complaintResponseRepository.save(new ComplaintResponse(
                complaint,
                responderUserId,
                request.getResponseContent().trim(),
                Boolean.TRUE.equals(request.getIsPublic()),
                now
        ));

        complaintEventPublisher.publishComplaintResponseRegistered(new ComplaintResponseRegisteredPayload(
                complaint.getId(),
                complaint.getComplaintNo(),
                complaintResponse.getId(),
                complaint.getApplicantUserId(),
                responderUserId,
                complaintResponse.isPublic(),
                toOffsetDateTime(complaintResponse.getRespondedAt()),
                complaint.getAssignedDepartmentId(),
                notifyChannelsFor(complaint)
        ));

        return new RegisterComplaintResponseResponse(
                complaintResponse.getId(),
                complaint.getId(),
                complaintResponse.isPublic(),
                complaintResponse.getRespondedAt()
        );
    }

    private List<NotifyChannel> notifyChannelsFor(Complaint complaint) {
        return complaint.isEmailNotificationEnabled()
                ? List.of(NotifyChannel.IN_APP, NotifyChannel.EMAIL)
                : List.of(NotifyChannel.IN_APP);
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
