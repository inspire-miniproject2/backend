package com.gcivil.complaint.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintResponse;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.repository.ComplaintRepository;
import com.gcivil.complaint.repository.ComplaintResponseRepository;
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
class PublicResponseQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintResponseRepository complaintResponseRepository;

    @BeforeEach
    void setUp() {
        complaintResponseRepository.deleteAll();
        complaintRepository.deleteAll();
    }

    @Test
    void getPublicResponsesReturnsOnlyPublicCompletedResponses() throws Exception {
        Complaint completedPublic = complaintRepository.save(completedComplaint(
                "CIV-2026-000601",
                "어린이보호구역 신호시간 조정 요청 처리 결과",
                "TRAFFIC",
                "교통정책과",
                LocalDateTime.of(2026, 8, 13, 15, 0)
        ));
        complaintResponseRepository.save(new ComplaintResponse(
                completedPublic,
                201L,
                "등교 시간대 보행 신호를 연장하기로 결정했습니다.",
                true,
                LocalDateTime.of(2026, 8, 13, 15, 10)
        ));

        Complaint completedPrivate = complaintRepository.save(completedComplaint(
                "CIV-2026-000602",
                "비공개 답변 민원",
                "TRAFFIC",
                "교통정책과",
                LocalDateTime.of(2026, 8, 14, 15, 0)
        ));
        complaintResponseRepository.save(new ComplaintResponse(
                completedPrivate,
                201L,
                "비공개 답변입니다.",
                false,
                LocalDateTime.of(2026, 8, 14, 15, 10)
        ));

        Complaint notCompleted = complaintRepository.save(inProgressComplaint(
                "CIV-2026-000603",
                "처리중 민원",
                "TRAFFIC",
                "교통정책과",
                LocalDateTime.of(2026, 8, 15, 15, 0)
        ));
        complaintResponseRepository.save(new ComplaintResponse(
                notCompleted,
                201L,
                "아직 처리중입니다.",
                true,
                LocalDateTime.of(2026, 8, 15, 15, 10)
        ));

        mockMvc.perform(get("/api/v1/public-responses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공개 답변 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("어린이보호구역 신호시간 조정 요청 처리 결과"))
                .andExpect(jsonPath("$.data.content[0].departmentName").value("교통정책과"))
                .andExpect(jsonPath("$.data.content[0].statusLabel").value("답변 완료"));
    }

    @Test
    void getPublicResponsesAppliesFilters() throws Exception {
        Complaint traffic = complaintRepository.save(completedComplaint(
                "CIV-2026-000611",
                "신호시간 조정 요청 처리 결과",
                "TRAFFIC",
                "교통정책과",
                LocalDateTime.of(2026, 8, 13, 15, 0)
        ));
        complaintResponseRepository.save(new ComplaintResponse(
                traffic,
                201L,
                "신호 시간을 조정했습니다.",
                true,
                LocalDateTime.of(2026, 8, 13, 15, 10)
        ));

        Complaint environment = complaintRepository.save(completedComplaint(
                "CIV-2026-000612",
                "쓰레기 수거 요청 처리 결과",
                "ENVIRONMENT",
                "환경정책과",
                LocalDateTime.of(2026, 8, 18, 9, 0)
        ));
        complaintResponseRepository.save(new ComplaintResponse(
                environment,
                301L,
                "수거 일정을 조정했습니다.",
                true,
                LocalDateTime.of(2026, 8, 18, 9, 10)
        ));

        mockMvc.perform(get("/api/v1/public-responses")
                        .param("keyword", "신호")
                        .param("categoryCode", "traffic")
                        .param("completedFrom", "2026-08-01")
                        .param("completedTo", "2026-08-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("신호시간 조정 요청 처리 결과"));
    }

    @Test
    void getPublicResponsesRejectsTooLongKeyword() throws Exception {
        mockMvc.perform(get("/api/v1/public-responses")
                        .param("keyword", "a".repeat(51)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    private Complaint completedComplaint(
            String complaintNo,
            String title,
            String categoryCode,
            String departmentName,
            LocalDateTime completedAt
    ) {
        Complaint complaint = new Complaint(
                complaintNo,
                101L,
                1L,
                categoryCode,
                title,
                "내용",
                ComplaintStatus.RECEIVED,
                completedAt.minusDays(1),
                completedAt.minusDays(1)
        );
        complaint.markAssigned(departmentName, 10L, 201L, completedAt.minusHours(2));
        complaint.markInProgress(completedAt.minusHours(1));
        complaint.markCompleted(completedAt);
        return complaint;
    }

    private Complaint inProgressComplaint(
            String complaintNo,
            String title,
            String categoryCode,
            String departmentName,
            LocalDateTime updatedAt
    ) {
        Complaint complaint = new Complaint(
                complaintNo,
                101L,
                1L,
                categoryCode,
                title,
                "내용",
                ComplaintStatus.RECEIVED,
                updatedAt.minusDays(1),
                updatedAt.minusDays(1)
        );
        complaint.markAssigned(departmentName, 10L, 201L, updatedAt.minusHours(2));
        complaint.markInProgress(updatedAt);
        return complaint;
    }
}
