package com.gcivil.assignment.repository;

import com.gcivil.assignment.domain.DepartmentCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentCategoryRepository extends JpaRepository<DepartmentCategory, Long> {

    @Query("""
            select dc
            from DepartmentCategory dc
            join fetch dc.department d
            join fetch dc.category c
            where dc.active = true
              and d.active = true
              and c.id = :categoryId
              and c.categoryCode = :categoryCode
            """)
    Optional<DepartmentCategory> findActiveRule(
            @Param("categoryId") Long categoryId,
            @Param("categoryCode") String categoryCode
    );
}
