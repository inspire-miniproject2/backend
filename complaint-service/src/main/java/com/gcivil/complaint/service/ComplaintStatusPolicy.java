package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ComplaintStatusPolicy {

    public void validateCompletionAllowed(boolean responseExists) {
        if (!responseExists) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "RESPONSE_REQUIRED",
                    "COMPLETED 상태로 변경하려면 공식 답변이 먼저 등록되어야 합니다."
            );
        }
    }

    public void validateResponseRegistrationAllowed(ComplaintStatus currentStatus) {
        if (currentStatus != ComplaintStatus.ASSIGNED && currentStatus != ComplaintStatus.IN_PROGRESS) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INVALID_STATUS_TRANSITION",
                    "공식 답변은 ASSIGNED 또는 IN_PROGRESS 상태에서만 등록할 수 있습니다."
            );
        }
    }

    public boolean isAutomaticTransition(ComplaintStatus previousStatus, ComplaintStatus newStatus) {
        return previousStatus == ComplaintStatus.RECEIVED && newStatus == ComplaintStatus.ASSIGNED;
    }
}
