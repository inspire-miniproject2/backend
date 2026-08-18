package com.gcivil.complaint.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcivil.complaint.domain.Complaint;
import com.gcivil.complaint.domain.ComplaintAttachment;
import com.gcivil.complaint.domain.ComplaintCategory;
import com.gcivil.complaint.domain.ComplaintResponse;
import com.gcivil.complaint.domain.ComplaintStatus;
import com.gcivil.complaint.repository.ComplaintCategoryRepository;
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
class PublicResponseDetailQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintResponseRepository complaintResponseRepository;

    @Autowired
    private ComplaintCategoryRepository complaintCategoryRepository;

    @BeforeEach
    void setUp() {
        complaintResponseRepository.deleteAll();
        complaintRepository.deleteAll();
        complaintCategoryRepository.deleteAll();
    }

    @Test
    void getPublicResponseDetailReturnsPublicResponse() throws Exception {
        complaintCategoryRepository.save(new ComplaintCategory(
                "도로·교통",
                "TRAFFIC",
                true,
                LocalDateTime.of(2026, 8, 18, 9, 0)
        ));

        Complaint complaint = completedComplaint(
                "CIV-2026-000701",
                "어린이보호구역 신호시간 조정 요청",
                "TRAFFIC",
                "교통정책과",
                LocalDateTime.of(2026, 8, 13, 15, 0)
        );
        complaint.addAttachment(new ComplaintAttachment(
                "교통량조사결과.pdf",
                "stored.pdf",
                "complaints/CIV-2026-000701/stored.pdf",
                "application/pdf",
                20480L,
                LocalDateTime.of(2026, 8, 13, 14, 0)
        ));
        complaint = complaintRepository.save(complaint);

        ComplaintResponse response = complaintResponseRepository.save(new ComplaintResponse(
                complaint,
                201L,
                "현장 교통량과 보행량 조사를 실시한 결과, 등교 시간대 보행 신호를 연장하기로 결정했습니다.",
                true,
                LocalDateTime.of(2026, 8, 13, 15, 10)
        ));

        mockMvc.perform(get("/api/v1/public-responses/{responseId}", response.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공개 답변 상세를 조회했습니다."))
                .andExpect(jsonPath("$.data.caseTitle").value("어린이보호구역 신호시간 조정 요청"))
                .andExpect(jsonPath("$.data.categoryName").value("도로·교통"))
                .andExpect(jsonPath("$.data.departmentName").value("교통정책과"))
                .andExpect(jsonPath("$.data.responseContent").value("현장 교통량과 보행량 조사를 실시한 결과, 등교 시간대 보행 신호를 연장하기로 결정했습니다."))
                .andExpect(jsonPath("$.data.attachments.length()").value(1))
                .andExpect(jsonPath("$.data.attachments[0].originalFilename").value("교통량조사결과.pdf"));
    }

    @Test
    void getPublicResponseDetailReturnsNotFoundForPrivateResponse() throws Exception {
        complaintCategoryRepository.save(new ComplaintCategory(
                "도로·교통",
                "TRAFFIC",
                true,
                LocalDateTime.of(2026, 8, 18, 9, 0)
        ));
        Complaint complaint = complaintRepository.save(completedComplaint(
                "CIV-2026-000702",
                "비공개 민원",
                "TRAFFIC",
                "교통정책과",
                LocalDateTime.of(2026, 8, 13, 15, 0)
        ));
        ComplaintResponse response = complaintResponseRepository.save(new ComplaintResponse(
                complaint,
                201L,
                "비공개 답변입니다.",
                false,
                LocalDateTime.of(2026, 8, 13, 15, 10)
        ));

        mockMvc.perform(get("/api/v1/public-responses/{responseId}", response.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getPublicResponseDetailReturnsNotFoundForUnknownResponse() throws Exception {
        mockMvc.perform(get("/api/v1/public-responses/{responseId}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
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
}
