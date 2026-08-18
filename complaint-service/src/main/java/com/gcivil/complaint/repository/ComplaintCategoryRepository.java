package com.gcivil.complaint.repository;

import com.gcivil.complaint.domain.ComplaintCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory, Long> {
    List<ComplaintCategory> findAllByActiveTrueOrderByIdAsc();

    List<ComplaintCategory> findAllByOrderByIdAsc();

    Optional<ComplaintCategory> findByCategoryCode(String categoryCode);
}
