package com.gcivil.complaint.service;

import com.gcivil.complaint.dto.ComplaintCategoryResponse;
import com.gcivil.complaint.repository.ComplaintCategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplaintCategoryQueryService {

    private final ComplaintCategoryRepository complaintCategoryRepository;

    public ComplaintCategoryQueryService(ComplaintCategoryRepository complaintCategoryRepository) {
        this.complaintCategoryRepository = complaintCategoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ComplaintCategoryResponse> getCategories(boolean activeOnly) {
        return (activeOnly
                ? complaintCategoryRepository.findAllByActiveTrueOrderByIdAsc()
                : complaintCategoryRepository.findAllByOrderByIdAsc())
                .stream()
                .map(category -> new ComplaintCategoryResponse(
                        category.getId(),
                        category.getCategoryName(),
                        category.getCategoryCode()
                ))
                .toList();
    }
}
