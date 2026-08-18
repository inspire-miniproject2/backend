package com.gcivil.complaint.service;

import com.gcivil.complaint.client.dto.AssignmentResponse;
import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintAttachment;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.domain.NotifyChannel;
import com.gcivil.complaint.dto.CreateComplaintRequest;
import com.gcivil.complaint.dto.CreateComplaintResponse;
import com.gcivil.complaint.event.ComplaintCreatedPayload;
import com.gcivil.complaint.event.ComplaintEventPublisher;
import com.gcivil.complaint.event.ComplaintStatusChangedPayload;
import com.gcivil.complaint.exception.ApiException;
import com.gcivil.complaint.repository.ComplaintRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ComplaintCommandService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintNumberGenerator complaintNumberGenerator;
    private final ComplaintStatusHistoryService complaintStatusHistoryService;
    private final AssignmentIntegrationService assignmentIntegrationService;
    private final ComplaintEventPublisher complaintEventPublisher;

    public ComplaintCommandService(
            ComplaintRepository complaintRepository,
            ComplaintNumberGenerator complaintNumberGenerator,
            ComplaintStatusHistoryService complaintStatusHistoryService,
            AssignmentIntegrationService assignmentIntegrationService,
            ComplaintEventPublisher complaintEventPublisher
    ) {
        this.complaintRepository = complaintRepository;
        this.complaintNumberGenerator = complaintNumberGenerator;
        this.complaintStatusHistoryService = complaintStatusHistoryService;
        this.assignmentIntegrationService = assignmentIntegrationService;
        this.complaintEventPublisher = complaintEventPublisher;
    }

    @Transactional
    public CreateComplaintResponse createComplaint(CreateComplaintRequest request, Long applicantUserId, String requestId) {
        validateNotifyChannels(request.getNotifyChannels());

        LocalDateTime now = LocalDateTime.now();
        Complaint complaint = new Complaint(
                complaintNumberGenerator.nextComplaintNo(now.toLocalDate()),
                applicantUserId,
                request.getCategoryId(),
                request.getCategoryCode().trim(),
                request.getTitle().trim(),
                request.getContent().trim(),
                ComplaintStatus.RECEIVED,
                now,
                now
        );

        for (MultipartFile attachmentFile : request.getAttachmentFiles()) {
            if (attachmentFile == null || attachmentFile.isEmpty()) {
                continue;
            }
            complaint.addAttachment(toAttachment(complaint.getComplaintNo(), attachmentFile, now));
        }

        complaintRepository.save(complaint);
        complaintStatusHistoryService.record(complaint, null, ComplaintStatus.RECEIVED, applicantUserId, "민원 접수", now);

        List<NotifyChannel> notifyChannels = normalizeNotifyChannels(request.getNotifyChannels());
        ComplaintCreatedPayload createdPayload = new ComplaintCreatedPayload(
                complaint.getId(),
                complaint.getComplaintNo(),
                complaint.getApplicantUserId(),
                complaint.getCategoryId(),
                complaint.getCategoryCode(),
                ComplaintStatus.RECEIVED.name(),
                toOffsetDateTime(complaint.getSubmittedAt()),
                toEventNotifyChannels(notifyChannels)
        );
        complaintEventPublisher.publishComplaintCreated(createdPayload);

        AssignmentResponse assignmentResponse = assignmentIntegrationService.requestAssignment(complaint, requestId);
        if (assignmentResponse.assignmentFound()) {
            complaint.markAssigned(
                    assignmentResponse.departmentId(),
                    assignmentResponse.officerUserId(),
                    assignmentResponse.assignedAt() == null ? now : assignmentResponse.assignedAt()
            );
            complaintStatusHistoryService.record(
                    complaint,
                    ComplaintStatus.RECEIVED,
                    ComplaintStatus.ASSIGNED,
                    null,
                    "자동 배정 완료",
                    complaint.getAssignedAt()
            );
            ComplaintStatusChangedPayload statusChangedPayload = new ComplaintStatusChangedPayload(
                    complaint.getId(),
                    complaint.getComplaintNo(),
                    complaint.getApplicantUserId(),
                    complaint.getCategoryId(),
                    complaint.getCategoryCode(),
                    ComplaintStatus.RECEIVED.name(),
                    ComplaintStatus.ASSIGNED.name(),
                    complaint.getAssignedDepartmentId(),
                    complaint.getAssignedOfficerUserId(),
                    null,
                    toOffsetDateTime(complaint.getAssignedAt()),
                    null,
                    null,
                    toEventNotifyChannels(notifyChannels)
            );
            complaintEventPublisher.publishComplaintStatusChanged(statusChangedPayload);
        }

        return new CreateComplaintResponse(
                complaint.getId(),
                complaint.getComplaintNo(),
                complaint.getCategoryCode(),
                complaint.getCurrentStatus(),
                complaint.getAssignedDepartmentId(),
                complaint.getAssignedOfficerUserId(),
                complaint.getSubmittedAt()
        );
    }

    private ComplaintAttachment toAttachment(String complaintNo, MultipartFile attachmentFile, LocalDateTime now) {
        String originalFilename = attachmentFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "첨부파일 이름이 비어 있습니다.");
        }
        String extension = "";
        int extensionIndex = originalFilename.lastIndexOf('.');
        if (extensionIndex >= 0) {
            extension = originalFilename.substring(extensionIndex);
        }
        String storedFilename = UUID.randomUUID() + extension.toLowerCase(Locale.ROOT);
        String filePath = "complaints/%s/%s".formatted(complaintNo, storedFilename);
        return new ComplaintAttachment(
                originalFilename,
                storedFilename,
                filePath,
                attachmentFile.getContentType() == null ? "application/octet-stream" : attachmentFile.getContentType(),
                attachmentFile.getSize(),
                now
        );
    }

    private void validateNotifyChannels(List<String> rawChannels) {
        if (rawChannels == null) {
            return;
        }
        for (String rawChannel : rawChannels) {
            if (rawChannel == null || rawChannel.isBlank()) {
                continue;
            }
            String normalized = rawChannel.trim().toUpperCase(Locale.ROOT);
            if (!NotifyChannel.EMAIL.name().equals(normalized)) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "VALIDATION_ERROR",
                        "notifyChannels 는 EMAIL 만 선택할 수 있습니다."
                );
            }
        }
    }

    private List<NotifyChannel> normalizeNotifyChannels(List<String> rawChannels) {
        Set<NotifyChannel> channels = new LinkedHashSet<>();
        channels.add(NotifyChannel.IN_APP);
        if (rawChannels != null) {
            for (String rawChannel : rawChannels) {
                if (rawChannel == null || rawChannel.isBlank()) {
                    continue;
                }
                if (NotifyChannel.EMAIL.name().equals(rawChannel.trim().toUpperCase(Locale.ROOT))) {
                    channels.add(NotifyChannel.EMAIL);
                }
            }
        }
        return List.copyOf(channels);
    }

    private List<com.gcivil.complaint.event.NotifyChannel> toEventNotifyChannels(List<NotifyChannel> channels) {
        return channels.stream()
                .map(channel -> com.gcivil.complaint.event.NotifyChannel.valueOf(channel.name()))
                .toList();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
