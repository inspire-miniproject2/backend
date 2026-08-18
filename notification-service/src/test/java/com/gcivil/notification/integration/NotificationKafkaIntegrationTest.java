package com.gcivil.notification.integration;

import com.gcivil.notification.domain.NotificationRepository;
import com.gcivil.notification.domain.ProcessedEventRepository;
import com.gcivil.notification.event.ComplaintStatusChangedPayload;
import com.gcivil.notification.event.EventEnvelope;
import com.gcivil.notification.event.NotifyChannel;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:notification-integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.enable-auto-commit=false",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false"
})
@EmbeddedKafka(partitions = 1, topics = {
        "complaint.status.changed.v1",
        "complaint.status.changed.v1.dlt",
        "complaint.response.registered.v1",
        "complaint.response.registered.v1.dlt"
}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class NotificationKafkaIntegrationTest {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @AfterEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void consumesStatusEventAndStoresNotificationOnce() throws Exception {
        UUID eventId = UUID.randomUUID();
        var event = statusEvent(eventId, 501L);

        kafkaTemplate.send("complaint.status.changed.v1", "1001", event).get();
        kafkaTemplate.send("complaint.status.changed.v1", "1001", event).get();

        await(() -> notificationRepository.count() == 1 && processedEventRepository.count() == 1,
                Duration.ofSeconds(10));
        assertThat(notificationRepository.findAll().get(0).getUserId()).isEqualTo(501L);
    }

    @Test
    void publishesPermanentlyFailingEventToDlt() throws Exception {
        Consumer<String, String> dltConsumer = dltConsumer("notification-dlt-test");
        try {
            kafkaTemplate.send("complaint.status.changed.v1", "1001",
                    statusEvent(UUID.randomUUID(), null)).get();

            ConsumerRecord<String, String> dltRecord = KafkaTestUtils.getSingleRecord(
                    dltConsumer, "complaint.status.changed.v1.dlt", Duration.ofSeconds(15));

            assertThat(dltRecord.topic()).isEqualTo("complaint.status.changed.v1.dlt");
            assertThat(dltRecord.partition()).isZero();
            assertThat(dltRecord.headers()).isNotEmpty();
        } finally {
            dltConsumer.close();
        }
    }

    private EventEnvelope<ComplaintStatusChangedPayload> statusEvent(UUID eventId, Long applicantUserId) {
        OffsetDateTime changedAt = OffsetDateTime.parse("2026-08-15T09:31:00+09:00");
        var payload = new ComplaintStatusChangedPayload(
                1001L, "CIV-2026-000184", applicantUserId, 10L, "ROAD", "RECEIVED", "ASSIGNED",
                21L, 9001L, null, changedAt, null, null, List.of(NotifyChannel.IN_APP));
        return new EventEnvelope<>(eventId, "ComplaintStatusChanged", "v1", changedAt,
                "complaint-service", "1001", payload);
    }

    private Consumer<String, String> dltConsumer(String groupId) {
        Map<String, Object> properties = KafkaTestUtils.consumerProps(groupId, "false", embeddedKafka);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                properties, new StringDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafka.consumeFromAnEmbeddedTopic(consumer, "complaint.status.changed.v1.dlt");
        return consumer;
    }

    private void await(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(100L);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
