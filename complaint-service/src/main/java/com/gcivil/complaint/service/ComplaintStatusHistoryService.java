package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.domain.ComplaintStatusHistory;
import com.gcivil.complaint.repository.ComplaintStatusHistoryRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class ComplaintStatusHistoryService {

    private final ComplaintStatusHistoryRepository complaintStatusHistoryRepository;

    public ComplaintStatusHistoryService(ComplaintStatusHistoryRepository complaintStatusHistoryRepository) {
        this.complaintStatusHistoryRepository = complaintStatusHistoryRepository;
    }

    public ComplaintStatusHistory record(
            Complaint complaint,
            ComplaintStatus previousStatus,
            ComplaintStatus newStatus,
            Long changedByUserId,
            String changeMemo,
            LocalDateTime changedAt
    ) {
        ComplaintStatusHistory history = new ComplaintStatusHistory(
                complaint,
                previousStatus,
                newStatus,
                changedByUserId,
                changeMemo,
                changedAt
        );
        return complaintStatusHistoryRepository.save(history);
    }
}
