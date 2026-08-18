package com.gcivil.complaint.event;

import java.time.OffsetDateTime;
import java.util.UUID;

record ComplaintEventMessage<T>(
        UUID eventId,
        String topic,
        String eventType,
        OffsetDateTime occurredAt,
        String partitionKey,
        T payload
) {
}
