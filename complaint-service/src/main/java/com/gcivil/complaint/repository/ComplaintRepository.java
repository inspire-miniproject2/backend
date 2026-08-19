package com.gcivil.complaint.repository;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Optional<Complaint> findTopByComplaintNoStartingWithOrderByComplaintNoDesc(String prefix);

    long countByAssignedOfficerUserIdAndCurrentStatus(Long assignedOfficerUserId, ComplaintStatus currentStatus);

    long countByCurrentStatus(ComplaintStatus currentStatus);

    long countByApplicantUserId(Long applicantUserId);

    long countByApplicantUserIdAndCurrentStatus(Long applicantUserId, ComplaintStatus currentStatus);

    @Query("""
            select c from Complaint c
            where c.applicantUserId = :applicantUserId
              and (:categoryCode is null or c.categoryCode = :categoryCode)
              and (:status is null or c.currentStatus = :status)
              and (
                    :keyword is null
                    or lower(c.complaintNo) like lower(concat('%', :keyword, '%'))
                    or lower(c.title) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Complaint> searchMyComplaints(
            @Param("applicantUserId") Long applicantUserId,
            @Param("categoryCode") String categoryCode,
            @Param("status") ComplaintStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            select c from Complaint c
            where (:assignedOfficerUserId is null or c.assignedOfficerUserId = :assignedOfficerUserId)
              and (:status is null or c.currentStatus = :status)
              and (
                    :keyword is null
                    or lower(c.complaintNo) like lower(concat('%', :keyword, '%'))
                    or lower(c.title) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<Complaint> searchOfficerComplaints(
            @Param("assignedOfficerUserId") Long assignedOfficerUserId,
            @Param("status") ComplaintStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

}
