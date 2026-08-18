package com.gcivil.statistics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class StatisticsServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
