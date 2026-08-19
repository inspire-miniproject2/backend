package com.gcivil.complaint.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcivil.complaint.client.UserServiceClient;
import com.gcivil.complaint.client.dto.InternalUserResponse;
import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.repository.ComplaintRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ComplaintOfficerQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintRepository complaintRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        complaintRepository.deleteAll();
    }

    @Test
    void getAssignedComplaintsReturnsSummaryAndContent() throws Exception {
        complaintRepository.save(complaint(
                "CIV-2026-000101",
                "어린이보호구역 신호시간 조정 요청",
                ComplaintStatus.ASSIGNED,
                201L,
                10L,
                LocalDateTime.of(2026, 8, 18, 9, 0)
        ));
        complaintRepository.save(complaint(
                "CIV-2026-000102",
                "횡단보도 보행신호 개선 요청",
                ComplaintStatus.IN_PROGRESS,
                201L,
                10L,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        ));
        complaintRepository.save(complaint(
                "CIV-2026-000103",
                "학교 앞 속도저감 시설 보강 요청",
                ComplaintStatus.COMPLETED,
                201L,
                10L,
                LocalDateTime.of(2026, 8, 18, 11, 0)
        ));
        complaintRepository.save(complaint(
                "CIV-2026-000104",
                "다른 담당자 민원",
                ComplaintStatus.ASSIGNED,
                202L,
                11L,
                LocalDateTime.of(2026, 8, 18, 12, 0)
        ));
        given(userServiceClient.getUser(eq(201L), eq("complaint-service"), anyString()))
                .willReturn(ApiResponse.success(
                        new InternalUserResponse(201L, "officer201", "김담당", "OFFICER", 10L, true),
                        "내부 사용자 정보를 조회했습니다."
                ));

        mockMvc.perform(get("/api/v1/officer/complaints")
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("담당 민원 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data.summary.newAssigned").value(1))
                .andExpect(jsonPath("$.data.summary.inProgress").value(1))
                .andExpect(jsonPath("$.data.summary.completed").value(1))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].complaintNo").value("CIV-2026-000103"))
                .andExpect(jsonPath("$.data.content[0].assigneeName").value("김담당"))
                .andExpect(jsonPath("$.data.content[0].assigneeUserId").value(201))
                .andExpect(jsonPath("$.data.content[0].assignedDepartmentId").value(10));
    }

    @Test
    void getAssignedComplaintsAppliesStatusAndKeywordFilters() throws Exception {
        complaintRepository.save(complaint(
                "CIV-2026-000201",
                "신호시간 조정 요청",
                ComplaintStatus.IN_PROGRESS,
                301L,
                12L,
                LocalDateTime.of(2026, 8, 18, 9, 0)
        ));
        complaintRepository.save(complaint(
                "CIV-2026-000202",
                "주정차 단속 요청",
                ComplaintStatus.IN_PROGRESS,
                301L,
                12L,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        ));
        complaintRepository.save(complaint(
                "CIV-2026-000203",
                "신호기 점검 요청",
                ComplaintStatus.COMPLETED,
                301L,
                12L,
                LocalDateTime.of(2026, 8, 18, 11, 0)
        ));
        given(userServiceClient.getUser(eq(301L), eq("complaint-service"), anyString()))
                .willReturn(ApiResponse.success(
                        new InternalUserResponse(301L, "officer301", "박처리", "OFFICER", 12L, true),
                        "내부 사용자 정보를 조회했습니다."
                ));

        mockMvc.perform(get("/api/v1/officer/complaints")
                        .header("X-User-Id", "301")
                        .header("X-User-Role", "OFFICER")
                        .param("status", "IN_PROGRESS")
                        .param("keyword", "신호"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].complaintNo").value("CIV-2026-000201"))
                .andExpect(jsonPath("$.data.content[0].assigneeName").value("박처리"))
                .andExpect(jsonPath("$.data.content[0].status").value("IN_PROGRESS"));
    }

    @Test
    void adminGetsAllComplaintsRegardlessOfAssignee() throws Exception {
        complaintRepository.save(complaint(
                "CIV-2026-000301",
                "첫 번째 담당자 민원",
                ComplaintStatus.ASSIGNED,
                201L,
                10L,
                LocalDateTime.of(2026, 8, 18, 9, 0)
        ));
        complaintRepository.save(complaint(
                "CIV-2026-000302",
                "두 번째 담당자 민원",
                ComplaintStatus.IN_PROGRESS,
                202L,
                20L,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        ));
        complaintRepository.save(complaint(
                "CIV-2026-000303",
                "아직 배정되지 않은 민원",
                ComplaintStatus.RECEIVED,
                null,
                null,
                LocalDateTime.of(2026, 8, 18, 11, 0)
        ));
        given(userServiceClient.getUser(eq(201L), eq("complaint-service"), anyString()))
                .willReturn(ApiResponse.success(
                        new InternalUserResponse(201L, "officer201", "김담당", "OFFICER", 10L, true),
                        "내부 사용자 정보를 조회했습니다."
                ));
        given(userServiceClient.getUser(eq(202L), eq("complaint-service"), anyString()))
                .willReturn(ApiResponse.success(
                        new InternalUserResponse(202L, "officer202", "이담당", "OFFICER", 20L, true),
                        "내부 사용자 정보를 조회했습니다."
                ));

        mockMvc.perform(get("/api/v1/officer/complaints")
                        .header("X-User-Id", "900")
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.newAssigned").value(1))
                .andExpect(jsonPath("$.data.summary.inProgress").value(1))
                .andExpect(jsonPath("$.data.summary.completed").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(3))
                .andExpect(jsonPath("$.data.content[0].complaintNo").value("CIV-2026-000303"))
                .andExpect(jsonPath("$.data.content[1].assigneeName").value("이담당"))
                .andExpect(jsonPath("$.data.content[2].assigneeName").value("김담당"));
    }

    @Test
    void getAssignedComplaintsRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/officer/complaints")
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER")
                        .param("status", "DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getAssignedComplaintsRejectsCitizenRole() throws Exception {
        mockMvc.perform(get("/api/v1/officer/complaints")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "CITIZEN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private Complaint complaint(
            String complaintNo,
            String title,
            ComplaintStatus status,
            Long officerUserId,
            Long departmentId,
            LocalDateTime updatedAt
    ) {
        Complaint complaint = new Complaint(
                complaintNo,
                101L,
                1L,
                "TRAFFIC",
                title,
                "내용",
                status,
                updatedAt.minusHours(1),
                updatedAt.minusHours(1)
        );

        if (status == ComplaintStatus.ASSIGNED || status == ComplaintStatus.IN_PROGRESS || status == ComplaintStatus.COMPLETED) {
            complaint.markAssigned("테스트부서", departmentId, officerUserId, updatedAt.minusMinutes(30));
        }
        if (status == ComplaintStatus.IN_PROGRESS) {
            complaint.markInProgress(updatedAt.minusMinutes(10));
        }
        if (status == ComplaintStatus.COMPLETED) {
            complaint.markInProgress(updatedAt.minusMinutes(20));
            complaint.markCompleted(updatedAt);
        } else {
            complaint.touch(updatedAt);
        }

        return complaint;
    }
}
