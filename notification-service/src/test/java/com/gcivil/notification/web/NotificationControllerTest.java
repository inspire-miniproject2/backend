package com.gcivil.notification.web;

import com.gcivil.notification.application.NotificationNotFoundException;
import com.gcivil.notification.application.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NotificationControllerTest {
    private final NotificationService notificationService = mock(NotificationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
                .setControllerAdvice(new NotificationExceptionHandler())
                .build();
    }

    @Test
    void returnsNotificationsForHeaderUser() throws Exception {
        when(notificationService.findAll(501L, 0, 20, false))
                .thenReturn(new NotificationPageResponse(3, List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/notifications").header("X-User-Id", "501")
                        .param("isRead", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(3))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void preventsReadingAnotherUsersNotification() throws Exception {
        when(notificationService.markAsRead(501L, 99L)).thenThrow(new NotificationNotFoundException(99L));

        mockMvc.perform(patch("/api/v1/notifications/99/read").header("X-User-Id", "501"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Notification not found"));
    }
}
