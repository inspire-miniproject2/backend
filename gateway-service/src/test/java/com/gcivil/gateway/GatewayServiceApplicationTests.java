package com.gcivil.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
        "jwt.secret=local-test-secret-that-is-at-least-32-bytes-long",
        "jwt.issuer=minwonon-auth"})
class GatewayServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
