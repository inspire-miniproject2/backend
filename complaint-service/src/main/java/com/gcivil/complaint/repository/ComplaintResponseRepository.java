package com.gcivil.complaint.repository;

import com.gcivil.complaint.domain.ComplaintResponse;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintResponseRepository extends JpaRepository<ComplaintResponse, Long> {
    boolean existsByComplaint_Id(Long complaintId);
    Optional<ComplaintResponse> findByComplaint_Id(Long complaintId);
}
