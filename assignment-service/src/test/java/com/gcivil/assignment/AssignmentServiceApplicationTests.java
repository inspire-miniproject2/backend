package com.gcivil.assignment;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;

@ActiveProfiles("test")
@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class AssignmentServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
