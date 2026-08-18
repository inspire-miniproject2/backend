package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintResponse;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.dto.ChangeComplaintStatusRequest;
import com.gcivil.complaint.dto.ChangeComplaintStatusResponse;
import com.gcivil.complaint.event.ComplaintEventPublisher;
import com.gcivil.complaint.event.ComplaintStatusChangedPayload;
import com.gcivil.complaint.event.NotifyChannel;
import com.gcivil.complaint.exception.ApiException;
import com.gcivil.complaint.repository.ComplaintRepository;
import com.gcivil.complaint.repository.ComplaintResponseRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintStatusCommandService {

    private static final List<NotifyChannel> DEFAULT_NOTIFY_CHANNELS = List.of(NotifyChannel.IN_APP);

    private final ComplaintRepository complaintRepository;
    private final ComplaintResponseRepository complaintResponseRepository;
    private final ComplaintStatusHistoryService complaintStatusHistoryService;
    private final ComplaintStatusPolicy complaintStatusPolicy;
    private final ComplaintEventPublisher complaintEventPublisher;

    public ComplaintStatusCommandService(
            ComplaintRepository complaintRepository,
            ComplaintResponseRepository complaintResponseRepository,
            ComplaintStatusHistoryService complaintStatusHistoryService,
            ComplaintStatusPolicy complaintStatusPolicy,
            ComplaintEventPublisher complaintEventPublisher
    ) {
        this.complaintRepository = complaintRepository;
        this.complaintResponseRepository = complaintResponseRepository;
        this.complaintStatusHistoryService = complaintStatusHistoryService;
        this.complaintStatusPolicy = complaintStatusPolicy;
        this.complaintEventPublisher = complaintEventPublisher;
    }

    @Transactional
    public ChangeComplaintStatusResponse changeStatus(
            Long complaintId,
            ChangeComplaintStatusRequest request,
            Long changedByUserId
    ) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "민원을 찾을 수 없습니다."
                ));

        ComplaintStatus newStatus = parseStatus(request.getNewStatus());
        ComplaintStatus previousStatus = complaint.getCurrentStatus();
        complaintStatusPolicy.validateTransitionAllowed(previousStatus, newStatus);

        ComplaintResponse complaintResponse = complaintResponseRepository.findByComplaint_Id(complaintId).orElse(null);
        if (newStatus == ComplaintStatus.COMPLETED) {
            complaintStatusPolicy.validateCompletionAllowed(complaintResponse != null);
        }

        LocalDateTime now = LocalDateTime.now();
        if (newStatus == ComplaintStatus.IN_PROGRESS) {
            complaint.markInProgress(now);
        } else if (newStatus == ComplaintStatus.COMPLETED) {
            complaint.markCompleted(now);
        }

        String changeMemo = normalizeMemo(request.getChangeMemo());
        complaintStatusHistoryService.record(
                complaint,
                previousStatus,
                newStatus,
                changedByUserId,
                changeMemo,
                now
        );

        complaintEventPublisher.publishComplaintStatusChanged(new ComplaintStatusChangedPayload(
                complaint.getId(),
                complaint.getComplaintNo(),
                complaint.getApplicantUserId(),
                complaint.getCategoryId(),
                complaint.getCategoryCode(),
                previousStatus.name(),
                newStatus.name(),
                complaint.getAssignedDepartmentId(),
                complaint.getAssignedOfficerUserId(),
                changedByUserId,
                toOffsetDateTime(now),
                changeMemo,
                complaintResponse == null ? null : toOffsetDateTime(complaintResponse.getRespondedAt()),
                DEFAULT_NOTIFY_CHANNELS
        ));

        return new ChangeComplaintStatusResponse(previousStatus, newStatus, now);
    }

    private ComplaintStatus parseStatus(String rawStatus) {
        try {
            return ComplaintStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "newStatus 값이 올바르지 않습니다."
            );
        }
    }

    private String normalizeMemo(String changeMemo) {
        if (changeMemo == null || changeMemo.isBlank()) {
            return null;
        }
        return changeMemo.trim();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
