package com.gcivil.complaint.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.event.ComplaintEventPublisher;
import com.gcivil.complaint.repository.ComplaintRepository;
import com.gcivil.complaint.repository.ComplaintResponseRepository;
import com.gcivil.complaint.repository.ComplaintStatusHistoryRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ComplaintStatusCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintResponseRepository complaintResponseRepository;

    @Autowired
    private ComplaintStatusHistoryRepository complaintStatusHistoryRepository;

    @MockBean
    private ComplaintEventPublisher complaintEventPublisher;

    @BeforeEach
    void setUp() {
        complaintStatusHistoryRepository.deleteAll();
        complaintResponseRepository.deleteAll();
        complaintRepository.deleteAll();
    }

    @Test
    void changeStatusMovesAssignedToInProgress() throws Exception {
        Complaint complaint = complaintRepository.save(assignedComplaint());

        mockMvc.perform(patch("/api/v1/officer/complaints/{complaintId}/status", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER")
                        .header("X-Department-Id", "10")
                        .content(objectMapper.writeValueAsString(new StatusRequestFixture(
                                "IN_PROGRESS",
                                "현장 조사 착수"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previousStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.newStatus").value("IN_PROGRESS"));

        Complaint updatedComplaint = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertThat(updatedComplaint.getCurrentStatus()).isEqualTo(ComplaintStatus.IN_PROGRESS);
        verify(complaintEventPublisher).publishComplaintStatusChanged(any());
    }

    @Test
    void changeStatusRejectsAssignedToCompleted() throws Exception {
        Complaint complaint = complaintRepository.save(assignedComplaint());

        mockMvc.perform(patch("/api/v1/officer/complaints/{complaintId}/status", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER")
                        .header("X-Department-Id", "10")
                        .content(objectMapper.writeValueAsString(new StatusRequestFixture(
                                "COMPLETED",
                                "바로 완료 처리 시도"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));

        verify(complaintEventPublisher, never()).publishComplaintStatusChanged(any());
    }

    @Test
    void changeStatusRejectsCompletedWithoutResponse() throws Exception {
        Complaint complaint = complaintRepository.save(inProgressComplaint());

        mockMvc.perform(patch("/api/v1/officer/complaints/{complaintId}/status", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER")
                        .header("X-Department-Id", "10")
                        .content(objectMapper.writeValueAsString(new StatusRequestFixture(
                                "COMPLETED",
                                "답변 없이 완료 시도"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESPONSE_REQUIRED"));

        verify(complaintEventPublisher, never()).publishComplaintStatusChanged(any());
    }

    @Test
    void changeStatusCompletesComplaintWhenOfficialResponseExists() throws Exception {
        Complaint complaint = complaintRepository.save(inProgressComplaint());

        mockMvc.perform(post("/api/v1/officer/complaints/{complaintId}/response", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER")
                        .header("X-Department-Id", "10")
                        .content(objectMapper.writeValueAsString(new ResponseRequestFixture(
                                "현장 교통량과 보행량 분석 결과에 따라 등교 시간대 보행 신호를 연장하기로 결정했습니다.",
                                true
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/v1/officer/complaints/{complaintId}/status", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER")
                        .header("X-Department-Id", "10")
                        .content(objectMapper.writeValueAsString(new StatusRequestFixture(
                                "COMPLETED",
                                "조치 완료"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previousStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.newStatus").value("COMPLETED"));

        Complaint updatedComplaint = complaintRepository.findById(complaint.getId()).orElseThrow();
        assertThat(updatedComplaint.getCurrentStatus()).isEqualTo(ComplaintStatus.COMPLETED);
        assertThat(updatedComplaint.getCompletedAt()).isNotNull();
        verify(complaintEventPublisher).publishComplaintResponseRegistered(any());
        verify(complaintEventPublisher).publishComplaintStatusChanged(any());
    }

    @Test
    void changeStatusRejectsDifferentDepartmentOfficer() throws Exception {
        Complaint complaint = complaintRepository.save(assignedComplaint());

        mockMvc.perform(patch("/api/v1/officer/complaints/{complaintId}/status", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "202")
                        .header("X-User-Role", "OFFICER")
                        .header("X-Department-Id", "11")
                        .content(objectMapper.writeValueAsString(new StatusRequestFixture(
                                "IN_PROGRESS",
                                "다른 부서 처리 시도"
                        ))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(complaintEventPublisher, never()).publishComplaintStatusChanged(any());
    }

    private Complaint assignedComplaint() {
        Complaint complaint = new Complaint(
                "CIV-2026-000401",
                101L,
                1L,
                "TRAFFIC",
                "어린이보호구역 신호시간 조정 요청",
                "출근 시간대 차량 정체로 인해 보행 대기 시간이 과도하게 길어 조정 검토를 요청드립니다.",
                ComplaintStatus.RECEIVED,
                LocalDateTime.of(2026, 8, 18, 9, 0),
                LocalDateTime.of(2026, 8, 18, 9, 0)
        );
        complaint.markAssigned("교통정책과", 10L, 201L, LocalDateTime.of(2026, 8, 18, 9, 10));
        return complaint;
    }

    private Complaint inProgressComplaint() {
        Complaint complaint = assignedComplaint();
        complaint.markInProgress(LocalDateTime.of(2026, 8, 18, 9, 20));
        return complaint;
    }

    private record StatusRequestFixture(
            String newStatus,
            String changeMemo
    ) {
    }

    private record ResponseRequestFixture(
            String responseContent,
            boolean isPublic
    ) {
    }
}
