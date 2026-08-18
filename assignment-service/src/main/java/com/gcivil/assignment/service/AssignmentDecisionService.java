package com.gcivil.assignment.service;

import com.gcivil.assignment.api.dto.AssignmentRequest;
import com.gcivil.assignment.api.dto.AssignmentResponse;
import com.gcivil.assignment.client.UserServiceClient;
import com.gcivil.assignment.client.dto.ApiSuccessResponse;
import com.gcivil.assignment.client.dto.InternalUserResponse;
import com.gcivil.assignment.config.AssignmentInternalProperties;
import com.gcivil.assignment.domain.DepartmentCategory;
import com.gcivil.assignment.domain.Role;
import com.gcivil.assignment.exception.ApiException;
import com.gcivil.assignment.repository.DepartmentCategoryRepository;
import feign.FeignException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AssignmentDecisionService {

    private final DepartmentCategoryRepository departmentCategoryRepository;
    private final UserServiceClient userServiceClient;
    private final AssignmentInternalProperties properties;

    public AssignmentDecisionService(
            DepartmentCategoryRepository departmentCategoryRepository,
            UserServiceClient userServiceClient,
            AssignmentInternalProperties properties
    ) {
        this.departmentCategoryRepository = departmentCategoryRepository;
        this.userServiceClient = userServiceClient;
        this.properties = properties;
    }

    public AssignmentResponse assign(AssignmentRequest request, String requestId) {
        return departmentCategoryRepository.findActiveRule(request.categoryId(), request.categoryCode())
                .map(rule -> buildAssignmentResponse(rule, requestId))
                .orElseGet(() -> AssignmentResponse.miss(
                        "NO_MATCHING_RULE",
                        "활성화된 카테고리-부서 매핑 규칙이 없습니다."
                ));
    }

    private AssignmentResponse buildAssignmentResponse(DepartmentCategory rule, String requestId) {
        InternalUserResponse officer = fetchOfficer(rule.getOfficerUserId(), requestId);
        if (!isAssignableOfficer(rule, officer)) {
            return AssignmentResponse.miss(
                    "NO_ACTIVE_OFFICER",
                    "배정 가능한 활성 담당 공무원이 없습니다."
            );
        }

        return AssignmentResponse.success(
                rule.getDepartment().getId(),
                rule.getDepartment().getDepartmentName(),
                officer.userId(),
                officer.name(),
                rule.getId(),
                LocalDateTime.now()
        );
    }

    private InternalUserResponse fetchOfficer(Long officerUserId, String requestId) {
        try {
            ApiSuccessResponse<InternalUserResponse> response = userServiceClient.getUser(
                    officerUserId,
                    properties.getUserServiceCaller(),
                    normalizeRequestId(requestId)
            );
            if (response == null || !response.success() || response.data() == null) {
                throw new ApiException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "SERVICE_UNAVAILABLE",
                        "user-service 응답이 올바르지 않습니다."
                );
            }
            return response.data();
        } catch (FeignException.NotFound ex) {
            return null;
        } catch (FeignException ex) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "SERVICE_UNAVAILABLE",
                    "user-service 호출에 실패했습니다."
            );
        }
    }

    private boolean isAssignableOfficer(DepartmentCategory rule, InternalUserResponse officer) {
        if (officer == null || !officer.isActive()) {
            return false;
        }
        if (!Role.OFFICER.name().equals(officer.role())) {
            return false;
        }
        return rule.getDepartment().getId().equals(officer.departmentId());
    }

    private String normalizeRequestId(String requestId) {
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }
}
