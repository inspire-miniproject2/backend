package com.gcivil.complaint.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcivil.complaint.client.AssignmentServiceClient;
import com.gcivil.complaint.client.dto.AssignmentResponse;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class ComplaintAttachmentDownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintAttachmentRepository complaintAttachmentRepository;

    @Autowired
    private ComplaintStatusHistoryRepository complaintStatusHistoryRepository;

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
    void applicantCanDownloadOwnAttachment() throws Exception {
        given(assignmentServiceClient.assign(any(), eq("complaint-service"), any()))
                .willReturn(ApiResponse.success(
                        new AssignmentResponse(false, null, null, null, null, null, null, "NO_MATCHING_RULE", "규칙 없음"),
                        "미배정"
                ));

        MockMultipartFile attachment = new MockMultipartFile(
                "attachmentFiles",
                "evidence.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "download me".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/complaints")
                        .file(attachment)
                        .param("categoryId", "1")
                        .param("categoryCode", "TRAFFIC")
                        .param("title", "첨부파일 다운로드 테스트")
                        .param("content", "첨부파일 다운로드 동작을 충분한 길이로 테스트합니다.")
                        .header("X-User-Id", "101"))
                .andExpect(status().isCreated());

        Long complaintId = complaintRepository.findAll().get(0).getId();
        Long attachmentId = complaintAttachmentRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/v1/complaints/{complaintId}/attachments/{attachmentId}", complaintId, attachmentId)
                        .header("X-User-Id", "101"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("filename=\"evidence.txt\"")))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().bytes("download me".getBytes()));
    }
}
