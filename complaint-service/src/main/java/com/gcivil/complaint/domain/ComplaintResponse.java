package com.gcivil.complaint.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_responses")
public class ComplaintResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    @Column(name = "responder_user_id", nullable = false)
    private Long responderUserId;

    @Column(name = "response_content", nullable = false, columnDefinition = "TEXT")
    private String responseContent;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "responded_at", nullable = false)
    private LocalDateTime respondedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected ComplaintResponse() {
    }
}
