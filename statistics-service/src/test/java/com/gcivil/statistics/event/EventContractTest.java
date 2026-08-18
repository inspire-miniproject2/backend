package com.gcivil.statistics.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EventContractTest {
    private final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Test
    void deserializesComplaintCreatedContract() throws Exception {
        String json = """
                {
                  "eventId":"4d46c02f-cce8-4afd-9cfa-14fceb56ae89",
                  "eventType":"ComplaintCreated",
                  "eventVersion":"v1",
                  "occurredAt":"2026-08-15T09:30:00+09:00",
                  "producer":"complaint-service",
                  "partitionKey":"1001",
                  "payload":{
                    "complaintId":1001,"complaintNo":"CIV-2026-000184","applicantUserId":501,
                    "categoryId":10,"categoryCode":"ROAD","currentStatus":"RECEIVED",
                    "submittedAt":"2026-08-15T09:30:00+09:00","notifyChannels":["IN_APP","EMAIL"]
                  }
                }
                """;

        EventEnvelope<ComplaintCreatedPayload> event = objectMapper.readValue(
                json, new TypeReference<>() { });

        assertThat(event.eventType()).isEqualTo("ComplaintCreated");
        assertThat(event.payload().categoryCode()).isEqualTo("ROAD");
        assertThat(event.payload().notifyChannels()).containsExactly(NotifyChannel.IN_APP, NotifyChannel.EMAIL);
    }
}
