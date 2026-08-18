package com.gcivil.complaint.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventContractTest {
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void serializesAndDeserializesStatusChangedEnvelope() throws Exception {
        UUID eventId = UUID.fromString("4d46c02f-cce8-4afd-9cfa-14fceb56ae89");
        OffsetDateTime occurredAt = OffsetDateTime.parse("2026-08-15T09:31:00+09:00");
        var payload = new ComplaintStatusChangedPayload(
                1001L, "CIV-2026-000184", 501L, 10L, "ROAD", "RECEIVED", "ASSIGNED",
                21L, 9001L, null, occurredAt, null, null,
                List.of(NotifyChannel.IN_APP, NotifyChannel.EMAIL));
        var event = new EventEnvelope<>(eventId, ComplaintEventTypes.COMPLAINT_STATUS_CHANGED,
                ComplaintEventTypes.VERSION, occurredAt, ComplaintEventTypes.PRODUCER, "1001", payload);

        String json = objectMapper.writeValueAsString(event);
        EventEnvelope<ComplaintStatusChangedPayload> restored = objectMapper.readValue(
                json, new TypeReference<>() { });

        assertThat(restored.eventId()).isEqualTo(event.eventId());
        assertThat(restored.eventType()).isEqualTo(event.eventType());
        assertThat(restored.payload().applicantUserId()).isEqualTo(501L);
        assertThat(restored.payload().categoryCode()).isEqualTo("ROAD");
        assertThat(restored.occurredAt().toInstant()).isEqualTo(event.occurredAt().toInstant());
        assertThat(restored.payload().statusChangedAt().toInstant())
                .isEqualTo(event.payload().statusChangedAt().toInstant());
        assertThat(restored.payload().notifyChannels()).isEqualTo(event.payload().notifyChannels());
        assertThat(objectMapper.readTree(json).path("payload").path("statusChangedByUserId").isNull()).isTrue();
        assertThat(objectMapper.readTree(json).path("occurredAt").asText()).isEqualTo("2026-08-15T09:31:00+09:00");
    }

    @Test
    void responseVisibilityUsesIsPublicJsonField() throws Exception {
        var payload = new ComplaintResponseRegisteredPayload(
                1001L, "CIV-2026-000184", 7001L, 501L, 9001L, true,
                OffsetDateTime.parse("2026-08-15T10:00:00+09:00"), 21L, List.of(NotifyChannel.IN_APP));

        assertThat(objectMapper.readTree(objectMapper.writeValueAsString(payload)).path("isPublic").asBoolean()).isTrue();
    }
}
