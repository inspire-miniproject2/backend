package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintAttachment;
import com.gcivil.complaint.exception.ApiException;
import com.gcivil.complaint.repository.ComplaintAttachmentRepository;
import com.gcivil.complaint.repository.ComplaintRepository;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttachmentDownloadService {

    private final ComplaintRepository complaintRepository;
    private final ComplaintAttachmentRepository complaintAttachmentRepository;
    private final AttachmentStorage attachmentStorage;

    public AttachmentDownloadService(
            ComplaintRepository complaintRepository,
            ComplaintAttachmentRepository complaintAttachmentRepository,
            AttachmentStorage attachmentStorage
    ) {
        this.complaintRepository = complaintRepository;
        this.complaintAttachmentRepository = complaintAttachmentRepository;
        this.attachmentStorage = attachmentStorage;
    }

    @Transactional(readOnly = true)
    public DownloadedAttachment download(
            Long complaintId,
            Long attachmentId,
            Long requesterUserId,
            String requesterRole
    ) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "민원을 찾을 수 없습니다."));

        validateAccess(complaint, requesterUserId, requesterRole);

        ComplaintAttachment attachment = complaintAttachmentRepository.findByIdAndComplaint_Id(attachmentId, complaintId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "첨부파일을 찾을 수 없습니다."));

        try {
            Resource resource = attachmentStorage.load(attachment.getFilePath());
            if (resource == null || !resource.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "첨부파일 원본을 찾을 수 없습니다.");
            }
            return new DownloadedAttachment(
                    attachment.getOriginalFilename(),
                    attachment.getContentType(),
                    resource
            );
        } catch (ApiException ex) {
            throw ex;
        } catch (IllegalStateException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "현재 첨부파일 저장소를 사용할 수 없습니다.");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "첨부파일을 읽는 중 오류가 발생했습니다.");
        }
    }

    private void validateAccess(Complaint complaint, Long requesterUserId, String requesterRole) {
        String normalizedRole = requesterRole == null || requesterRole.isBlank()
                ? "CITIZEN"
                : requesterRole.trim().toUpperCase(Locale.ROOT);
        if ("OFFICER".equals(normalizedRole) || "ADMIN".equals(normalizedRole)) {
            return;
        }
        if (!complaint.getApplicantUserId().equals(requesterUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "해당 민원 첨부파일에 접근할 권한이 없습니다.");
        }
    }

    public record DownloadedAttachment(
            String originalFilename,
            String contentType,
            Resource resource
    ) {
    }
}
