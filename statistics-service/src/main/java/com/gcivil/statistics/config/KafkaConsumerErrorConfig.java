package com.gcivil.statistics.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaConsumerErrorConfig {
    private final Map<String, String> dltByTopic;

    public KafkaConsumerErrorConfig(
            @Value("${gcivil.kafka.topics.complaint-created:complaint.created.v1}") String createdTopic,
            @Value("${gcivil.kafka.topics.complaint-created-dlt:complaint.created.v1.dlt}") String createdDlt,
            @Value("${gcivil.kafka.topics.complaint-status-changed:complaint.status.changed.v1}") String statusTopic,
            @Value("${gcivil.kafka.topics.complaint-status-changed-dlt:complaint.status.changed.v1.dlt}") String statusDlt
    ) {
        this.dltByTopic = Map.of(createdTopic, createdDlt, statusTopic, statusDlt);
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> resolveDlt(record.topic()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
    }

    TopicPartition resolveDlt(String sourceTopic) {
        String dlt = dltByTopic.getOrDefault(sourceTopic, sourceTopic + ".dlt");
        return new TopicPartition(dlt, 0);
    }
}
