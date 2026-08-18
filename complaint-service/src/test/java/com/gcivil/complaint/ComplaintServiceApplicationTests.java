package com.gcivil.complaint;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:complaint", "spring.flyway.enabled=false"})
class ComplaintServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
