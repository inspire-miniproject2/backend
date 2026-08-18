package com.gcivil.complaint.service;

import com.gcivil.complaint.repository.ComplaintRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class ComplaintNumberGenerator {

    private final ComplaintRepository complaintRepository;

    public ComplaintNumberGenerator(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    public String nextComplaintNo(LocalDate date) {
        String prefix = "CIV-%d-".formatted(date.getYear());
        String lastComplaintNo = complaintRepository.findTopByComplaintNoStartingWithOrderByComplaintNoDesc(prefix)
                .map(complaint -> complaint.getComplaintNo())
                .orElse(null);

        int nextSequence = 1;
        if (lastComplaintNo != null && lastComplaintNo.length() >= prefix.length() + 6) {
            nextSequence = Integer.parseInt(lastComplaintNo.substring(prefix.length())) + 1;
        }
        return prefix + "%06d".formatted(nextSequence);
    }
}
