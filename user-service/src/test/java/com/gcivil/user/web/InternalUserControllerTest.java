package com.gcivil.user.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class InternalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsInternalUserWhenRequestIsValid() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/201")
                        .header("X-Internal-Caller", "assignment-service")
                        .header("X-Request-Id", "req-201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(201))
                .andExpect(jsonPath("$.data.loginId").value("officer01"))
                .andExpect(jsonPath("$.data.name").value("김담당"))
                .andExpect(jsonPath("$.data.role").value("OFFICER"))
                .andExpect(jsonPath("$.data.departmentId").value(10))
                .andExpect(jsonPath("$.data.active").doesNotExist())
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void returnsNotFoundWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/9999")
                        .header("X-Internal-Caller", "assignment-service"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void returnsEmailNotificationPreferenceForInternalCaller() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/201/notification-preference")
                        .header("X-Internal-Caller", "notification-service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(201))
                .andExpect(jsonPath("$.data.email").value("officer01@gcivil.local"))
                .andExpect(jsonPath("$.data.emailNotifyAgreed").value(true))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void rejectsRequestWithoutInternalCallerHeader() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/201"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void rejectsNonPositiveUserId() throws Exception {
        mockMvc.perform(get("/api/v1/internal/users/0")
                        .header("X-Internal-Caller", "assignment-service"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }
}
