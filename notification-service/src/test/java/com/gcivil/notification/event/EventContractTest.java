package com.gcivil.notification.event;

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
    void deserializesResponseRegisteredContract() throws Exception {
        String json = """
                {
                  "eventId":"4d46c02f-cce8-4afd-9cfa-14fceb56ae89",
                  "eventType":"ComplaintResponseRegistered",
                  "eventVersion":"v1",
                  "occurredAt":"2026-08-15T10:00:00+09:00",
                  "producer":"complaint-service",
                  "partitionKey":"1001",
                  "payload":{
                    "complaintId":1001,"complaintNo":"CIV-2026-000184","responseId":7001,
                    "applicantUserId":501,"responderUserId":9001,"isPublic":true,
                    "respondedAt":"2026-08-15T10:00:00+09:00","assignedDepartmentId":21,
                    "notifyChannels":["IN_APP","EMAIL"]
                  }
                }
                """;

        EventEnvelope<ComplaintResponseRegisteredPayload> event = objectMapper.readValue(
                json, new TypeReference<>() { });

        assertThat(event.partitionKey()).isEqualTo("1001");
        assertThat(event.payload().applicantUserId()).isEqualTo(501L);
        assertThat(event.payload().notifyChannels()).containsExactly(NotifyChannel.IN_APP, NotifyChannel.EMAIL);
    }
}
