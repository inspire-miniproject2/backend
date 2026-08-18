package com.gcivil.complaint.web;

import com.gcivil.complaint.service.AttachmentDownloadService;
import jakarta.validation.constraints.Positive;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintAttachmentDownloadController {

    private final AttachmentDownloadService attachmentDownloadService;

    public ComplaintAttachmentDownloadController(AttachmentDownloadService attachmentDownloadService) {
        this.attachmentDownloadService = attachmentDownloadService;
    }

    @GetMapping("/{complaintId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable @Positive(message = "complaintId must be positive") Long complaintId,
            @PathVariable @Positive(message = "attachmentId must be positive") Long attachmentId,
            @RequestHeader("X-User-Id") @Positive(message = "X-User-Id must be positive") Long requesterUserId,
            @RequestHeader(value = "X-User-Role", required = false) String requesterRole
    ) {
        AttachmentDownloadService.DownloadedAttachment downloadedAttachment =
                attachmentDownloadService.download(complaintId, attachmentId, requesterUserId, requesterRole);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(downloadedAttachment.originalFilename())
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType(downloadedAttachment.contentType()))
                .body(downloadedAttachment.resource());
    }
}
