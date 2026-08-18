package com.gcivil.assignment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcivil.assignment.api.dto.AssignmentRequest;
import com.gcivil.assignment.client.UserServiceClient;
import com.gcivil.assignment.client.dto.ApiSuccessResponse;
import com.gcivil.assignment.client.dto.InternalUserResponse;
import java.time.LocalDateTime;
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
class InternalAssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserServiceClient userServiceClient;

    @Test
    void assignsDepartmentAndOfficerWhenRuleAndOfficerAreValid() throws Exception {
        AssignmentRequest request = new AssignmentRequest(
                5001L,
                "CIV-2026-000184",
                1L,
                "TRAFFIC",
                101L,
                LocalDateTime.of(2026, 8, 18, 10, 30)
        );

        given(userServiceClient.getUser(eq(201L), eq("assignment-service"), any()))
                .willReturn(new ApiSuccessResponse<>(
                        true,
                        new InternalUserResponse(201L, "officer01", "김담당", "OFFICER", 10L, true),
                        "ok"
                ));

        mockMvc.perform(post("/api/v1/internal/assignments")
                        .header("X-Internal-Caller", "complaint-service")
                        .header("X-Request-Id", "req-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignmentFound").value(true))
                .andExpect(jsonPath("$.data.departmentId").value(10))
                .andExpect(jsonPath("$.data.departmentName").value("교통정책과"))
                .andExpect(jsonPath("$.data.officerUserId").value(201))
                .andExpect(jsonPath("$.data.officerName").value("김담당"))
                .andExpect(jsonPath("$.data.assignmentRuleId").value(31));
    }

    @Test
    void returnsBusinessMissWhenNoRuleExists() throws Exception {
        AssignmentRequest request = new AssignmentRequest(
                5002L,
                "CIV-2026-000185",
                999L,
                "UNKNOWN",
                101L,
                LocalDateTime.of(2026, 8, 18, 10, 30)
        );

        mockMvc.perform(post("/api/v1/internal/assignments")
                        .header("X-Internal-Caller", "complaint-service")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assignmentFound").value(false))
                .andExpect(jsonPath("$.data.reasonCode").value("NO_MATCHING_RULE"));
    }

    @Test
    void returnsBusinessMissWhenOfficerIsInactive() throws Exception {
        AssignmentRequest request = new AssignmentRequest(
                5001L,
                "CIV-2026-000184",
                1L,
                "TRAFFIC",
                101L,
                LocalDateTime.of(2026, 8, 18, 10, 30)
        );

        given(userServiceClient.getUser(eq(201L), eq("assignment-service"), any()))
                .willReturn(new ApiSuccessResponse<>(
                        true,
                        new InternalUserResponse(201L, "officer01", "김담당", "OFFICER", 10L, false),
                        "ok"
                ));

        mockMvc.perform(post("/api/v1/internal/assignments")
                        .header("X-Internal-Caller", "complaint-service")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentFound").value(false))
                .andExpect(jsonPath("$.data.reasonCode").value("NO_ACTIVE_OFFICER"));
    }

    @Test
    void rejectsRequestWithoutInternalCallerHeader() throws Exception {
        AssignmentRequest request = new AssignmentRequest(
                5001L,
                "CIV-2026-000184",
                1L,
                "TRAFFIC",
                101L,
                LocalDateTime.of(2026, 8, 18, 10, 30)
        );

        mockMvc.perform(post("/api/v1/internal/assignments")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void validatesRequiredFields() throws Exception {
        String invalidPayload = """
                {
                  "complaintId": 5001,
                  "complaintNo": "",
                  "categoryId": 1,
                  "categoryCode": "TRAFFIC",
                  "applicantUserId": 101
                }
                """;

        mockMvc.perform(post("/api/v1/internal/assignments")
                        .header("X-Internal-Caller", "complaint-service")
                        .contentType("application/json")
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
