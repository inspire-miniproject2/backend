package com.gcivil.complaint.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gcivil.complaint.domain.ComplaintCategory;
import com.gcivil.complaint.repository.ComplaintCategoryRepository;
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
class ComplaintCategoryQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComplaintCategoryRepository complaintCategoryRepository;

    @BeforeEach
    void setUp() {
        complaintCategoryRepository.deleteAll();
    }

    @Test
    void getCategoriesReturnsActiveCategoriesByDefault() throws Exception {
        complaintCategoryRepository.save(new ComplaintCategory(
                "도로·교통",
                "TRAFFIC",
                true,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        ));
        complaintCategoryRepository.save(new ComplaintCategory(
                "기타",
                "ETC",
                false,
                LocalDateTime.of(2026, 8, 18, 10, 5)
        ));

        mockMvc.perform(get("/api/v1/complaint-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("카테고리 목록을 조회했습니다."))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].categoryName").value("도로·교통"))
                .andExpect(jsonPath("$.data[0].categoryCode").value("TRAFFIC"));
    }

    @Test
    void getCategoriesReturnsAllWhenActiveOnlyFalse() throws Exception {
        complaintCategoryRepository.save(new ComplaintCategory(
                "도로·교통",
                "TRAFFIC",
                true,
                LocalDateTime.of(2026, 8, 18, 10, 0)
        ));
        complaintCategoryRepository.save(new ComplaintCategory(
                "기타",
                "ETC",
                false,
                LocalDateTime.of(2026, 8, 18, 10, 5)
        ));

        mockMvc.perform(get("/api/v1/complaint-categories")
                        .param("activeOnly", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[1].categoryCode").value("ETC"));
    }
}
