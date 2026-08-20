package com.gcivil.assignment.web;

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
class DepartmentQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsAllActiveDepartmentsInIdOrder() throws Exception {
        mockMvc.perform(get("/api/v1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].departmentId").value(10))
                .andExpect(jsonPath("$.data[0].departmentName").value("교통정책과"))
                .andExpect(jsonPath("$.data[1].departmentId").value(20))
                .andExpect(jsonPath("$.data[1].departmentName").value("시설관리과"))
                .andExpect(jsonPath("$.data[2].departmentId").value(30))
                .andExpect(jsonPath("$.data[2].departmentName").value("환경관리과"))
                .andExpect(jsonPath("$.data[3].departmentId").value(40))
                .andExpect(jsonPath("$.data[3].departmentName").value("복지지원과"))
                .andExpect(jsonPath("$.data[4].departmentId").value(50))
                .andExpect(jsonPath("$.data[4].departmentName").value("민원총괄과"))
                .andExpect(jsonPath("$.message").value("부서 목록을 조회했습니다."));
    }
}
