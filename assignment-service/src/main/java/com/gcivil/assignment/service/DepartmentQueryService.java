package com.gcivil.assignment.service;

import com.gcivil.assignment.api.dto.DepartmentResponse;
import com.gcivil.assignment.repository.DepartmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentQueryService {

    private final DepartmentRepository departmentRepository;

    public DepartmentQueryService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> getActiveDepartments() {
        return departmentRepository.findAllByActiveTrueOrderByIdAsc().stream()
                .map(department -> new DepartmentResponse(
                        department.getId(),
                        department.getDepartmentName()
                ))
                .toList();
    }
}
