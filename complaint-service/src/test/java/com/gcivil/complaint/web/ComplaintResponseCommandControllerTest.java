package com.gcivil.complaint.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class ComplaintResponseCommandControllerTest {

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
    void registerResponseStoresOfficialResponseForAssignedComplaint() throws Exception {
        Complaint complaint = complaintRepository.save(assignedComplaint());

        mockMvc.perform(post("/api/v1/officer/complaints/{complaintId}/response", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .content(objectMapper.writeValueAsString(new RequestBodyFixture(
                                "현장 점검과 교통량 분석 결과에 따라 신호 시간을 조정하기로 결정했습니다.",
                                true
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.complaintId").value(complaint.getId()))
                .andExpect(jsonPath("$.data.isPublic").value(true))
                .andExpect(jsonPath("$.data.responseId").isNumber());

        assertThat(complaintResponseRepository.count()).isEqualTo(1);
        var response = complaintResponseRepository.findAll().get(0);
        assertThat(response.getComplaint().getId()).isEqualTo(complaint.getId());
        assertThat(response.getResponderUserId()).isEqualTo(201L);
        verify(complaintEventPublisher).publishComplaintResponseRegistered(any());
    }

    @Test
    void registerResponseRejectsDuplicateOfficialResponse() throws Exception {
        Complaint complaint = complaintRepository.save(assignedComplaint());

        mockMvc.perform(post("/api/v1/officer/complaints/{complaintId}/response", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .content(objectMapper.writeValueAsString(new RequestBodyFixture(
                                "현장 점검과 교통량 분석 결과에 따라 신호 시간을 조정하기로 결정했습니다.",
                                true
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/officer/complaints/{complaintId}/response", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .content(objectMapper.writeValueAsString(new RequestBodyFixture(
                                "두 번째 공식 답변은 허용되지 않아야 합니다. 이 요청은 실패해야 합니다.",
                                false
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));

        assertThat(complaintResponseRepository.count()).isEqualTo(1);
    }

    @Test
    void registerResponseRejectsReceivedComplaint() throws Exception {
        Complaint complaint = complaintRepository.save(receivedComplaint());

        mockMvc.perform(post("/api/v1/officer/complaints/{complaintId}/response", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .content(objectMapper.writeValueAsString(new RequestBodyFixture(
                                "접수 상태에서는 공식 답변을 바로 등록할 수 없으므로 이 요청은 실패해야 합니다.",
                                false
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));

        assertThat(complaintResponseRepository.count()).isZero();
        verify(complaintEventPublisher, never()).publishComplaintResponseRegistered(any());
    }

    @Test
    void registerResponseRejectsShortContent() throws Exception {
        Complaint complaint = complaintRepository.save(assignedComplaint());

        mockMvc.perform(post("/api/v1/officer/complaints/{complaintId}/response", complaint.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "201")
                        .content(objectMapper.writeValueAsString(new RequestBodyFixture("너무 짧음", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        assertThat(complaintResponseRepository.count()).isZero();
    }

    private Complaint assignedComplaint() {
        Complaint complaint = new Complaint(
                "CIV-2026-000301",
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

    private Complaint receivedComplaint() {
        return new Complaint(
                "CIV-2026-000302",
                101L,
                1L,
                "TRAFFIC",
                "어린이보호구역 신호시간 조정 요청",
                "출근 시간대 차량 정체로 인해 보행 대기 시간이 과도하게 길어 조정 검토를 요청드립니다.",
                ComplaintStatus.RECEIVED,
                LocalDateTime.of(2026, 8, 18, 9, 0),
                LocalDateTime.of(2026, 8, 18, 9, 0)
        );
    }

    private record RequestBodyFixture(
            String responseContent,
            boolean isPublic
    ) {
    }
}
