package com.gcivil.assignment.web;

import com.gcivil.assignment.api.dto.ApiResponse;
import com.gcivil.assignment.api.dto.DepartmentResponse;
import com.gcivil.assignment.service.DepartmentQueryService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/departments")
public class DepartmentQueryController {

    private final DepartmentQueryService departmentQueryService;

    public DepartmentQueryController(DepartmentQueryService departmentQueryService) {
        this.departmentQueryService = departmentQueryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> getDepartments() {
        List<DepartmentResponse> response = departmentQueryService.getActiveDepartments();
        return ResponseEntity.ok(ApiResponse.success(response, "부서 목록을 조회했습니다."));
    }
}
