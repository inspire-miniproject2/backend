package com.gcivil.notification.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        String eventVersion,
        OffsetDateTime occurredAt,
        String producer,
        String partitionKey,
        T payload
) {
}
