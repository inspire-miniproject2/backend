package com.gcivil.complaint.repository;

import com.gcivil.complaint.domain.ComplaintResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplaintResponseRepository extends JpaRepository<ComplaintResponse, Long> {
    boolean existsByComplaint_Id(Long complaintId);
    Optional<ComplaintResponse> findByComplaint_Id(Long complaintId);

    @Query("""
            select cr from ComplaintResponse cr
            join fetch cr.complaint c
            where cr.isPublic = true
              and c.currentStatus = com.gcivil.complaint.domain.ComplaintStatus.COMPLETED
              and (:keyword is null
                   or lower(c.title) like lower(concat('%', :keyword, '%'))
                   or lower(cr.responseContent) like lower(concat('%', :keyword, '%')))
              and (:categoryCode is null or c.categoryCode = :categoryCode)
              and (:completedFrom is null or c.completedAt >= :completedFrom)
              and (:completedTo is null or c.completedAt < :completedTo)
            order by c.completedAt desc, cr.id desc
            """)
    List<ComplaintResponse> searchPublicResponses(
            @Param("keyword") String keyword,
            @Param("categoryCode") String categoryCode,
            @Param("completedFrom") LocalDateTime completedFrom,
            @Param("completedTo") LocalDateTime completedTo
    );

    @Query("""
            select cr from ComplaintResponse cr
            join fetch cr.complaint c
            where cr.id = :responseId
              and cr.isPublic = true
              and c.currentStatus = com.gcivil.complaint.domain.ComplaintStatus.COMPLETED
            """)
    Optional<ComplaintResponse> findPublicResponseDetailById(@Param("responseId") Long responseId);
}
