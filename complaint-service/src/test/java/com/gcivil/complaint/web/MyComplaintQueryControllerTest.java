package com.gcivil.complaint.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.repository.ComplaintRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class MyComplaintQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintRepository complaintRepository;

    @BeforeEach
    void setUp() {
        complaintRepository.deleteAll();
    }

    @Test
    void getMyComplaintsReturnsSummaryAndContent() throws Exception {
        complaintRepository.save(receivedComplaint(
                "CIV-2026-000301",
                101L,
                "TRAFFIC",
                "신호시간 조정 요청",
                LocalDateTime.of(2026, 8, 18, 9, 0)
        ));
        complaintRepository.save(assignedComplaint(
                "CIV-2026-000302",
                101L,
                1L,
                "TRAFFIC",
                "횡단보도 보행신호 개선 요청",
                ComplaintStatus.IN_PROGRESS,
                "교통정책과",
                10L,
                201L,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        ));
        complaintRepository.save(assignedComplaint(
                "CIV-2026-000303",
                101L,
                3L,
                "FACILITY",
                "보도블록 정비 요청",
                ComplaintStatus.COMPLETED,
                "시설관리과",
                20L,
                202L,
                LocalDateTime.of(2026, 8, 18, 11, 0)
        ));
        complaintRepository.save(receivedComplaint(
                "CIV-2026-000304",
                102L,
                "TRAFFIC",
                "다른 사용자의 민원",
                LocalDateTime.of(2026, 8, 18, 12, 0)
        ));

        mockMvc.perform(get("/api/v1/complaints/my")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "CITIZEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 민원 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data.summary.total").value(3))
                .andExpect(jsonPath("$.data.summary.received").value(1))
                .andExpect(jsonPath("$.data.summary.assigned").value(0))
                .andExpect(jsonPath("$.data.summary.inProgress").value(1))
                .andExpect(jsonPath("$.data.summary.completed").value(1))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].complaintNo").value("CIV-2026-000303"))
                .andExpect(jsonPath("$.data.content[0].assignedDepartmentName").value("시설관리과"))
                .andExpect(jsonPath("$.data.content[1].assignedDepartmentName").value("교통정책과"))
                .andExpect(jsonPath("$.data.content[2].currentStatus").value("RECEIVED"));
    }

    @Test
    void getMyComplaintsAppliesFilters() throws Exception {
        complaintRepository.save(receivedComplaint(
                "CIV-2026-000401",
                101L,
                "TRAFFIC",
                "신호기 점검 요청",
                LocalDateTime.of(2026, 8, 18, 9, 0)
        ));
        complaintRepository.save(assignedComplaint(
                "CIV-2026-000402",
                101L,
                1L,
                "TRAFFIC",
                "신호시간 조정 요청",
                ComplaintStatus.IN_PROGRESS,
                "교통정책과",
                10L,
                201L,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        ));
        complaintRepository.save(assignedComplaint(
                "CIV-2026-000403",
                101L,
                3L,
                "FACILITY",
                "도로 포트홀 정비 요청",
                ComplaintStatus.IN_PROGRESS,
                "시설관리과",
                20L,
                202L,
                LocalDateTime.of(2026, 8, 18, 11, 0)
        ));

        mockMvc.perform(get("/api/v1/complaints/my")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "CITIZEN")
                        .param("status", "IN_PROGRESS")
                        .param("categoryCode", "traffic")
                        .param("keyword", "신호"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].complaintNo").value("CIV-2026-000402"))
                .andExpect(jsonPath("$.data.content[0].assignedDepartmentName").value("교통정책과"));
    }

    @Test
    void getMyComplaintsRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/complaints/my")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "CITIZEN")
                        .param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getMyComplaintsRejectsOfficerRole() throws Exception {
        mockMvc.perform(get("/api/v1/complaints/my")
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private Complaint receivedComplaint(
            String complaintNo,
            Long applicantUserId,
            String categoryCode,
            String title,
            LocalDateTime updatedAt
    ) {
        Complaint complaint = new Complaint(
                complaintNo,
                applicantUserId,
                1L,
                categoryCode,
                title,
                "내용",
                ComplaintStatus.RECEIVED,
                updatedAt.minusHours(1),
                updatedAt.minusHours(1)
        );
        complaint.touch(updatedAt);
        return complaint;
    }

    private Complaint assignedComplaint(
            String complaintNo,
            Long applicantUserId,
            Long categoryId,
            String categoryCode,
            String title,
            ComplaintStatus status,
            String departmentName,
            Long departmentId,
            Long officerUserId,
            LocalDateTime updatedAt
    ) {
        Complaint complaint = new Complaint(
                complaintNo,
                applicantUserId,
                categoryId,
                categoryCode,
                title,
                "내용",
                ComplaintStatus.RECEIVED,
                updatedAt.minusHours(2),
                updatedAt.minusHours(2)
        );
        complaint.markAssigned(departmentName, departmentId, officerUserId, updatedAt.minusMinutes(30));
        if (status == ComplaintStatus.IN_PROGRESS) {
            complaint.markInProgress(updatedAt);
        } else if (status == ComplaintStatus.COMPLETED) {
            complaint.markInProgress(updatedAt.minusMinutes(10));
            complaint.markCompleted(updatedAt);
        } else {
            complaint.touch(updatedAt);
        }
        return complaint;
    }
}
