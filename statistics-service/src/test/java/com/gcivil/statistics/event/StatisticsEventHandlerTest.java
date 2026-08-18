package com.gcivil.statistics.event;

import com.gcivil.statistics.domain.ComplaintStatisticSource;
import com.gcivil.statistics.domain.StatisticsAggregationRepository;
import com.gcivil.statistics.domain.ProcessedEventRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StatisticsEventHandlerTest {
    private final StatisticsAggregationRepository repository = mock(StatisticsAggregationRepository.class);
    private final ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
    private final StatisticsEventHandler handler = new StatisticsEventHandler(repository, processedEvents);

    @Test
    void createsSourceAndReceivedBucket() {
        OffsetDateTime submittedAt = OffsetDateTime.parse("2026-08-15T09:30:00+09:00");
        var payload = new ComplaintCreatedPayload(
                1001L, "CIV-2026-000184", 501L, 10L, "ROAD", "RECEIVED",
                submittedAt, List.of(NotifyChannel.IN_APP));
        var event = new EventEnvelope<>(UUID.randomUUID(), "ComplaintCreated", "v1", submittedAt,
                "complaint-service", "1001", payload);

        handler.handleCreated(event);

        verify(repository).insertSource(new ComplaintStatisticSource(
                1001L, LocalDate.parse("2026-08-15"), 0L, "ROAD", "RECEIVED"),
                submittedAt.toLocalDateTime());
        verify(repository).changeCount(LocalDate.parse("2026-08-15"), 0L, "ROAD", "RECEIVED", 1,
                submittedAt.toLocalDateTime());
        verify(processedEvents).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void movesCountFromPreviousStatusAndDepartmentToNewBucket() {
        OffsetDateTime changedAt = OffsetDateTime.parse("2026-08-15T09:31:00+09:00");
        when(repository.findSource(1001L)).thenReturn(Optional.of(new ComplaintStatisticSource(
                1001L, LocalDate.parse("2026-08-15"), 0L, "ROAD", "RECEIVED")));
        var payload = new ComplaintStatusChangedPayload(
                1001L, "CIV-2026-000184", 501L, 10L, "ROAD", "RECEIVED", "ASSIGNED",
                21L, 9001L, null, changedAt, null, null, List.of(NotifyChannel.IN_APP));
        var event = new EventEnvelope<>(UUID.randomUUID(), "ComplaintStatusChanged", "v1", changedAt,
                "complaint-service", "1001", payload);

        handler.handleStatusChanged(event);

        var order = inOrder(repository);
        order.verify(repository).changeCount(LocalDate.parse("2026-08-15"), 0L, "ROAD", "RECEIVED", -1,
                changedAt.toLocalDateTime());
        order.verify(repository).changeCount(LocalDate.parse("2026-08-15"), 21L, "ROAD", "ASSIGNED", 1,
                changedAt.toLocalDateTime());
        order.verify(repository).updateSource(1001L, 21L, "ASSIGNED", changedAt.toLocalDateTime());
        verify(processedEvents).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void ignoresAlreadyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        when(processedEvents.existsById(eventId.toString())).thenReturn(true);
        OffsetDateTime submittedAt = OffsetDateTime.parse("2026-08-15T09:30:00+09:00");
        var payload = new ComplaintCreatedPayload(
                1001L, "CIV-2026-000184", 501L, 10L, "ROAD", "RECEIVED",
                submittedAt, List.of(NotifyChannel.IN_APP));

        handler.handleCreated(new EventEnvelope<>(eventId, "ComplaintCreated", "v1", submittedAt,
                "complaint-service", "1001", payload));

        verify(repository, org.mockito.Mockito.never()).insertSource(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
