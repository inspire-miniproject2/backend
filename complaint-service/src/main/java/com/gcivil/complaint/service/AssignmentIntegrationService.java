package com.gcivil.complaint.service;

import com.gcivil.complaint.client.AssignmentServiceClient;
import com.gcivil.complaint.client.dto.AssignmentRequest;
import com.gcivil.complaint.client.dto.AssignmentResponse;
import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.dto.ApiResponse;
import feign.FeignException;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AssignmentIntegrationService {

    private static final String INTERNAL_CALLER = "complaint-service";

    private final AssignmentServiceClient assignmentServiceClient;

    public AssignmentIntegrationService(AssignmentServiceClient assignmentServiceClient) {
        this.assignmentServiceClient = assignmentServiceClient;
    }

    public AssignmentResponse requestAssignment(Complaint complaint, String requestId) {
        try {
            ApiResponse<AssignmentResponse> response = assignmentServiceClient.assign(
                    new AssignmentRequest(
                            complaint.getId(),
                            complaint.getComplaintNo(),
                            complaint.getCategoryId(),
                            complaint.getCategoryCode(),
                            complaint.getApplicantUserId(),
                            complaint.getSubmittedAt()
                    ),
                    INTERNAL_CALLER,
                    normalizeRequestId(requestId)
            );
            return response == null || response.data() == null
                    ? new AssignmentResponse(false, null, null, null, null, null, null, "ASSIGNMENT_UNAVAILABLE", "배정 서비스 응답이 비어 있습니다.")
                    : response.data();
        } catch (FeignException ex) {
            return new AssignmentResponse(false, null, null, null, null, null, null, "ASSIGNMENT_UNAVAILABLE", "배정 서비스 호출에 실패했습니다.");
        }
    }

    private String normalizeRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }
}
