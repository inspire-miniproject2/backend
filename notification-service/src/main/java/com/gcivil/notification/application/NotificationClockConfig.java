package com.gcivil.notification.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class NotificationClockConfig {
    @Bean
    Clock notificationClock() {
        return Clock.systemDefaultZone();
    }
}
