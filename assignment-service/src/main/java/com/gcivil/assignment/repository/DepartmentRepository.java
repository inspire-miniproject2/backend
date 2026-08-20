package com.gcivil.assignment.repository;

import com.gcivil.assignment.domain.Department;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByActiveTrueOrderByIdAsc();
}
