package com.gcivil.complaint.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/complaints")
public class ComplaintProbeController {
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "complaint-service", "status", "UP");
    }
}
