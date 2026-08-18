package com.gcivil.notification.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerErrorConfigTest {
    private final KafkaConsumerErrorConfig config = new KafkaConsumerErrorConfig(
            "complaint.status.changed.v1", "complaint.status.changed.v1.dlt",
            "complaint.response.registered.v1", "complaint.response.registered.v1.dlt");

    @Test
    void routesEachSourceTopicToSinglePartitionDlt() {
        assertThat(config.resolveDlt("complaint.status.changed.v1").topic())
                .isEqualTo("complaint.status.changed.v1.dlt");
        assertThat(config.resolveDlt("complaint.response.registered.v1").topic())
                .isEqualTo("complaint.response.registered.v1.dlt");
        assertThat(config.resolveDlt("complaint.status.changed.v1").partition()).isZero();
    }
}
