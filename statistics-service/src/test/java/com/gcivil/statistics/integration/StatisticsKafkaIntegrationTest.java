package com.gcivil.statistics.integration;

import com.gcivil.statistics.domain.ProcessedEventRepository;
import com.gcivil.statistics.event.ComplaintCreatedPayload;
import com.gcivil.statistics.event.ComplaintStatusChangedPayload;
import com.gcivil.statistics.event.EventEnvelope;
import com.gcivil.statistics.event.NotifyChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:statistics-integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
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
        "complaint.created.v1",
        "complaint.created.v1.dlt",
        "complaint.status.changed.v1",
        "complaint.status.changed.v1.dlt"
}, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Sql(statements = {
        "CREATE TABLE IF NOT EXISTS complaint_statistics (statistic_date DATE NOT NULL, department_id BIGINT NOT NULL, category_code VARCHAR(50) NOT NULL, status VARCHAR(30) NOT NULL, complaint_count BIGINT NOT NULL DEFAULT 0, updated_at TIMESTAMP NOT NULL, PRIMARY KEY (statistic_date, department_id, category_code, status))",
        "CREATE TABLE IF NOT EXISTS complaint_statistic_sources (complaint_id BIGINT NOT NULL PRIMARY KEY, statistic_date DATE NOT NULL, assigned_department_id BIGINT NOT NULL, category_code VARCHAR(50) NOT NULL, current_status VARCHAR(30) NOT NULL, updated_at TIMESTAMP NOT NULL)"
})
@DirtiesContext
class StatisticsKafkaIntegrationTest {
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @AfterEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM complaint_statistics");
        jdbcTemplate.update("DELETE FROM complaint_statistic_sources");
        processedEventRepository.deleteAll();
    }

    @Test
    void consumesCreatedAndStatusEventsAndAggregatesEachEventOnce() throws Exception {
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-15T09:30:00+09:00");
        EventEnvelope<ComplaintCreatedPayload> created = new EventEnvelope<>(
                UUID.randomUUID(), "ComplaintCreated", "v1", occurredAt,
                "complaint-service", "1001",
                new ComplaintCreatedPayload(1001L, "CIV-2026-000184", 501L, 10L,
                        "ROAD", "RECEIVED", occurredAt, List.of(NotifyChannel.IN_APP)));
        EventEnvelope<ComplaintStatusChangedPayload> changed = new EventEnvelope<>(
                UUID.randomUUID(), "ComplaintStatusChanged", "v1", occurredAt.plusMinutes(1),
                "complaint-service", "1001",
                new ComplaintStatusChangedPayload(1001L, "CIV-2026-000184", 501L, 10L,
                        "ROAD", "RECEIVED", "ASSIGNED", 21L, 9001L, null,
                        occurredAt.plusMinutes(1), null, null, List.of(NotifyChannel.IN_APP)));

        kafkaTemplate.send("complaint.created.v1", "1001", created).get();
        await(() -> processedEventRepository.count() == 1, Duration.ofSeconds(10));
        kafkaTemplate.send("complaint.created.v1", "1001", created).get();
        kafkaTemplate.send("complaint.status.changed.v1", "1001", changed).get();

        await(() -> processedEventRepository.count() == 2, Duration.ofSeconds(10));
        assertThat(countFor(0L, "RECEIVED")).isZero();
        assertThat(countFor(21L, "ASSIGNED")).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT assigned_department_id FROM complaint_statistic_sources WHERE complaint_id = 1001",
                Long.class)).isEqualTo(21L);
    }

    private long countFor(long departmentId, String status) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT complaint_count FROM complaint_statistics WHERE department_id = ? AND status = ?",
                Long.class, departmentId, status);
        return count == null ? 0 : count;
    }

    private void await(BooleanSupplier condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(100L);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
