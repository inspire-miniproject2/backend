package com.gcivil.user.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserProbeController {
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "user-service", "status", "UP");
    }
}
