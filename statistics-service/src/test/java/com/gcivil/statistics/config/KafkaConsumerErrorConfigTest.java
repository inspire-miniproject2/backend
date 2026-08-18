package com.gcivil.statistics.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerErrorConfigTest {
    private final KafkaConsumerErrorConfig config = new KafkaConsumerErrorConfig(
            "complaint.created.v1", "complaint.created.v1.dlt",
            "complaint.status.changed.v1", "complaint.status.changed.v1.dlt");

    @Test
    void routesEachSourceTopicToSinglePartitionDlt() {
        assertThat(config.resolveDlt("complaint.created.v1").topic())
                .isEqualTo("complaint.created.v1.dlt");
        assertThat(config.resolveDlt("complaint.status.changed.v1").topic())
                .isEqualTo("complaint.status.changed.v1.dlt");
        assertThat(config.resolveDlt("complaint.created.v1").partition()).isZero();
    }
}
