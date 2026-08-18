package com.gcivil.complaint.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintAttachment;
import com.gcivil.complaint.domain.ComplaintResponse;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.repository.ComplaintAttachmentRepository;
import com.gcivil.complaint.repository.ComplaintRepository;
import com.gcivil.complaint.repository.ComplaintResponseRepository;
import com.gcivil.complaint.repository.ComplaintStatusHistoryRepository;
import com.gcivil.complaint.service.ComplaintStatusHistoryService;
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
class ComplaintDetailQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintAttachmentRepository complaintAttachmentRepository;

    @Autowired
    private ComplaintResponseRepository complaintResponseRepository;

    @Autowired
    private ComplaintStatusHistoryRepository complaintStatusHistoryRepository;

    @Autowired
    private ComplaintStatusHistoryService complaintStatusHistoryService;

    @BeforeEach
    void setUp() {
        complaintStatusHistoryRepository.deleteAll();
        complaintResponseRepository.deleteAll();
        complaintAttachmentRepository.deleteAll();
        complaintRepository.deleteAll();
    }

    @Test
    void citizenCanReadOwnComplaintDetail() throws Exception {
        Complaint complaint = complaintRepository.save(complaint());
        complaint.addAttachment(new ComplaintAttachment(
                "현장사진.jpg",
                "stored.jpg",
                "complaints/CIV-2026-000501/stored.jpg",
                "image/jpeg",
                12345L,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        ));
        complaint.markAssigned("교통정책과", 10L, 201L, LocalDateTime.of(2026, 8, 18, 10, 10));
        complaint.markInProgress(LocalDateTime.of(2026, 8, 18, 10, 20));
        complaintRepository.save(complaint);
        complaintStatusHistoryService.record(complaint, null, ComplaintStatus.RECEIVED, 101L, "민원 접수", LocalDateTime.of(2026, 8, 18, 10, 0));
        complaintStatusHistoryService.record(complaint, ComplaintStatus.RECEIVED, ComplaintStatus.ASSIGNED, null, "자동 배정 완료", LocalDateTime.of(2026, 8, 18, 10, 10));
        complaintStatusHistoryService.record(complaint, ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS, 201L, "현장 확인 시작", LocalDateTime.of(2026, 8, 18, 10, 20));
        complaintResponseRepository.save(new ComplaintResponse(
                complaint,
                201L,
                "현장 교통량을 확인한 뒤 신호시간 조정을 검토 중입니다.",
                true,
                LocalDateTime.of(2026, 8, 18, 10, 30)
        ));
        complaintRepository.save(complaint);

        mockMvc.perform(get("/api/v1/complaints/{complaintId}", complaint.getId())
                        .header("X-User-Id", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("민원 상세를 조회했습니다."))
                .andExpect(jsonPath("$.data.complaintNo").value("CIV-2026-000501"))
                .andExpect(jsonPath("$.data.currentStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.assignedDepartmentName").value("교통정책과"))
                .andExpect(jsonPath("$.data.attachments.length()").value(1))
                .andExpect(jsonPath("$.data.attachments[0].originalFilename").value("현장사진.jpg"))
                .andExpect(jsonPath("$.data.response.responseId").exists())
                .andExpect(jsonPath("$.data.statusHistories.length()").value(3))
                .andExpect(jsonPath("$.data.statusHistories[1].newStatus").value("ASSIGNED"));
    }

    @Test
    void citizenCannotReadOthersComplaint() throws Exception {
        Complaint complaint = complaintRepository.save(complaint());

        mockMvc.perform(get("/api/v1/complaints/{complaintId}", complaint.getId())
                        .header("X-User-Id", "999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void officerCanReadOthersComplaint() throws Exception {
        Complaint complaint = complaintRepository.save(complaint());

        mockMvc.perform(get("/api/v1/complaints/{complaintId}", complaint.getId())
                        .header("X-User-Id", "201")
                        .header("X-User-Role", "OFFICER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.complaintNo").value("CIV-2026-000501"));
    }

    @Test
    void returnsNotFoundWhenComplaintDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/complaints/{complaintId}", 9999L)
                        .header("X-User-Id", "101"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    private Complaint complaint() {
        return new Complaint(
                "CIV-2026-000501",
                101L,
                1L,
                "TRAFFIC",
                "어린이보호구역 신호시간 조정 요청",
                "출근 시간대 차량 정체로 인해 보행 대기 시간이 길어 조정을 요청드립니다.",
                ComplaintStatus.RECEIVED,
                LocalDateTime.of(2026, 8, 18, 10, 0),
                LocalDateTime.of(2026, 8, 18, 10, 0)
        );
    }
}
