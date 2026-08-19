package com.gcivil.complaint.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(prefix = "gcivil.attachment.storage", name = "type", havingValue = "s3")
public class S3AttachmentStorageConfig {

    @Bean
    S3Client s3Client(AttachmentStorageProperties properties) {
        return S3Client.builder()
                .region(Region.of(properties.getAwsRegion()))
                .build();
    }
}
