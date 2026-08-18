package com.gcivil.complaint.repository;

import com.gcivil.complaint.domain.ComplaintStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintStatusHistoryRepository extends JpaRepository<ComplaintStatusHistory, Long> {
    List<ComplaintStatusHistory> findByComplaint_IdOrderByChangedAtAsc(Long complaintId);
}
