package com.gcivil.notification;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:notification", "spring.flyway.enabled=false"})
class NotificationServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
