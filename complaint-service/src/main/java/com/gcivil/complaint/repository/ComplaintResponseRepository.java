package com.gcivil.complaint.repository;

import com.gcivil.complaint.domain.ComplaintResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintResponseRepository extends JpaRepository<ComplaintResponse, Long> {
    boolean existsByComplaint_Id(Long complaintId);
}
