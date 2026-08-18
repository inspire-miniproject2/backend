package com.gcivil.complaint.repository;

import com.gcivil.complaint.domain.Complaint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Optional<Complaint> findTopByComplaintNoStartingWithOrderByComplaintNoDesc(String prefix);
}
