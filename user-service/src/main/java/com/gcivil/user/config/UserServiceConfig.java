package com.gcivil.user.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        InternalUserProperties.class,
        AuthProperties.class
})
public class UserServiceConfig {
}
