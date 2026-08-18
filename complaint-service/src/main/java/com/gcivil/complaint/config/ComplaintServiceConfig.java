package com.gcivil.complaint.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        KafkaTopicProperties.class,
        AttachmentStorageProperties.class
})
public class ComplaintServiceConfig {
}
