package com.gcivil.assignment.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assignments")
public class AssignmentProbeController {
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "assignment-service", "status", "UP");
    }
}
