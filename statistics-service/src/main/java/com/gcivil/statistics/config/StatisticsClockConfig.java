package com.gcivil.statistics.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StatisticsClockConfig {
    @Bean
    Clock statisticsClock() {
        return Clock.systemDefaultZone();
    }
}
