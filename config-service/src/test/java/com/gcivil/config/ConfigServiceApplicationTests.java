package com.gcivil.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "native"})
@SpringBootTest
class ConfigServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
