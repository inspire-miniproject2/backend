package com.gcivil.complaint.service;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.exception.ApiException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class UserAccessGuard {

    public void requireCitizen(String requesterRole) {
        String normalizedRole = normalizeRole(requesterRole);
        if (!"CITIZEN".equals(normalizedRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "민원인 권한이 필요합니다.");
        }
    }

    public void requireOfficerOrAdmin(String requesterRole) {
        String normalizedRole = normalizeRole(requesterRole);
        if (!"OFFICER".equals(normalizedRole) && !"ADMIN".equals(normalizedRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "공무원 또는 관리자 권한이 필요합니다.");
        }
    }

    public void requireAssignedDepartmentOrAdmin(Complaint complaint, String requesterRole, Long departmentId) {
        String normalizedRole = normalizeRole(requesterRole);
        if ("ADMIN".equals(normalizedRole)) {
            return;
        }
        if (!"OFFICER".equals(normalizedRole)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "공무원 또는 관리자 권한이 필요합니다.");
        }
        if (departmentId == null
                || complaint.getAssignedDepartmentId() == null
                || !complaint.getAssignedDepartmentId().equals(departmentId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "해당 민원을 처리할 권한이 없습니다.");
        }
    }

    public String normalizeRole(String requesterRole) {
        if (requesterRole == null || requesterRole.isBlank()) {
            return "CITIZEN";
        }
        return requesterRole.trim().toUpperCase(Locale.ROOT);
    }
}
