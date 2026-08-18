package com.gcivil.notification.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationProbeController {
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("service", "notification-service", "status", "UP");
    }
}
