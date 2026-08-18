package com.gcivil.complaint.repository;

import com.gcivil.complaint.domain.ComplaintAttachment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintAttachmentRepository extends JpaRepository<ComplaintAttachment, Long> {
    long countByComplaint_Id(Long complaintId);

    Optional<ComplaintAttachment> findByIdAndComplaint_Id(Long attachmentId, Long complaintId);
}
