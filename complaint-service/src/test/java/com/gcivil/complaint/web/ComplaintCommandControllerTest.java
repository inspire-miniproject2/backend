package com.gcivil.complaint.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcivil.complaint.client.AssignmentServiceClient;
import com.gcivil.complaint.client.dto.AssignmentResponse;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.dto.ApiResponse;
import com.gcivil.complaint.event.ComplaintEventPublisher;
import com.gcivil.complaint.repository.ComplaintAttachmentRepository;
import com.gcivil.complaint.repository.ComplaintRepository;
import com.gcivil.complaint.repository.ComplaintStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ComplaintCommandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintStatusHistoryRepository complaintStatusHistoryRepository;

    @Autowired
    private ComplaintAttachmentRepository complaintAttachmentRepository;

    @MockBean
    private AssignmentServiceClient assignmentServiceClient;

    @MockBean
    private ComplaintEventPublisher complaintEventPublisher;

    @BeforeEach
    void setUp() {
        complaintStatusHistoryRepository.deleteAll();
        complaintAttachmentRepository.deleteAll();
        complaintRepository.deleteAll();
    }

    @Test
    void createComplaintAssignsOfficerAndStoresAttachments() throws Exception {
        given(assignmentServiceClient.assign(any(), eq("complaint-service"), any()))
                .willReturn(ApiResponse.success(
                        new AssignmentResponse(
                                true,
                                10L,
                                "교통정책과",
                                201L,
                                "김담당",
                                31L,
                                java.time.LocalDateTime.of(2026, 8, 18, 10, 30),
                                null,
                                null
                        ),
                        "배정 성공"
                ));

        MockMultipartFile attachment = new MockMultipartFile(
                "attachmentFiles",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "image".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/complaints")
                        .file(attachment)
                        .param("categoryId", "1")
                        .param("categoryCode", "TRAFFIC")
                        .param("title", "어린이보호구역 신호시간 조정 요청")
                        .param("content", "출근 시간대 차량 정체로 인해 보행 대기 시간이 과도하게 길어 조정 검토를 요청드립니다.")
                        .param("notifyChannels", "EMAIL")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "CITIZEN")
                        .header("X-Request-Id", "req-complaint-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentStatus").value("ASSIGNED"))
                .andExpect(jsonPath("$.data.assignedDepartmentId").value(10))
                .andExpect(jsonPath("$.data.assignedOfficerUserId").value(201));

        var storedComplaint = complaintRepository.findAll().get(0);
        org.assertj.core.api.Assertions.assertThat(storedComplaint.getCurrentStatus()).isEqualTo(ComplaintStatus.ASSIGNED);
        org.assertj.core.api.Assertions.assertThat(complaintAttachmentRepository.countByComplaint_Id(storedComplaint.getId())).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(
                complaintStatusHistoryRepository.findByComplaint_IdOrderByChangedAtAsc(storedComplaint.getId())
        ).hasSize(2);

        verify(complaintEventPublisher).publishComplaintCreated(any());
        verify(complaintEventPublisher).publishComplaintStatusChanged(any());
    }

    @Test
    void createComplaintKeepsReceivedWhenAssignmentMisses() throws Exception {
        given(assignmentServiceClient.assign(any(), eq("complaint-service"), any()))
                .willReturn(ApiResponse.success(
                        new AssignmentResponse(false, null, null, null, null, null, null, "NO_MATCHING_RULE", "규칙 없음"),
                        "미배정"
                ));

        mockMvc.perform(multipart("/api/v1/complaints")
                        .param("categoryId", "1")
                        .param("categoryCode", "TRAFFIC")
                        .param("title", "어린이보호구역 신호시간 조정 요청")
                        .param("content", "출근 시간대 차량 정체로 인해 보행 대기 시간이 과도하게 길어 조정 검토를 요청드립니다.")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "CITIZEN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.currentStatus").value("RECEIVED"))
                .andExpect(jsonPath("$.data.assignedDepartmentId").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.assignedOfficerUserId").value(org.hamcrest.Matchers.nullValue()));

        var storedComplaint = complaintRepository.findAll().get(0);
        org.assertj.core.api.Assertions.assertThat(storedComplaint.getCurrentStatus()).isEqualTo(ComplaintStatus.RECEIVED);
        org.assertj.core.api.Assertions.assertThat(
                complaintStatusHistoryRepository.findByComplaint_IdOrderByChangedAtAsc(storedComplaint.getId())
        ).hasSize(1);
        verify(complaintEventPublisher).publishComplaintCreated(any());
        verify(complaintEventPublisher, never()).publishComplaintStatusChanged(any());
    }

    @Test
    void createComplaintRejectsInvalidNotifyChannel() throws Exception {
        mockMvc.perform(multipart("/api/v1/complaints")
                        .param("categoryId", "1")
                        .param("categoryCode", "TRAFFIC")
                        .param("title", "어린이보호구역 신호시간 조정 요청")
                        .param("content", "출근 시간대 차량 정체로 인해 보행 대기 시간이 과도하게 길어 조정 검토를 요청드립니다.")
                        .param("notifyChannels", "SMS")
                        .header("X-User-Id", "101")
                        .header("X-User-Role", "CITIZEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createComplaintRejectsOfficerRole() throws Exception {
        mockMvc.perform(multipart("/api/v1/complaints")
                        .param("categoryId", "1")
                        .param("categoryCode", "TRAFFIC")
                        .param("title", "어린이보호구역 신호시간 조정 요청")
                        .param("content", "출근 시간대 차량 정체로 인해 보행 대기 시간이 과도하게 길어 조정 검토를 요청드립니다.")
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
}
