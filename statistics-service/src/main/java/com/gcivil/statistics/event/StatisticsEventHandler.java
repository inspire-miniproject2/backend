package com.gcivil.statistics.event;

import com.gcivil.statistics.domain.ComplaintStatisticSource;
import com.gcivil.statistics.domain.StatisticsAggregationRepository;
import com.gcivil.statistics.domain.ProcessedEvent;
import com.gcivil.statistics.domain.ProcessedEventRepository;
import com.gcivil.statistics.config.StatisticsMetricsProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsEventHandler {
    private static final long UNASSIGNED_DEPARTMENT = 0L;

    private final StatisticsAggregationRepository repository;
    private final ProcessedEventRepository processedEventRepository;
    private final StatisticsMetricsProperties metricsProperties;

    public StatisticsEventHandler(StatisticsAggregationRepository repository,
                                  ProcessedEventRepository processedEventRepository,
                                  StatisticsMetricsProperties metricsProperties) {
        this.repository = repository;
        this.processedEventRepository = processedEventRepository;
        this.metricsProperties = metricsProperties;
    }

    @Transactional
    public void handleCreated(EventEnvelope<ComplaintCreatedPayload> event) {
        if (isProcessed(event)) {
            return;
        }
        var payload = event.payload();
        var submittedAt = payload.submittedAt().toLocalDateTime();
        var source = new ComplaintStatisticSource(
                payload.complaintId(), payload.submittedAt().toLocalDate(), UNASSIGNED_DEPARTMENT,
                payload.categoryCode(), payload.currentStatus(), submittedAt,
                submittedAt.plusDays(metricsProperties.getProcessingDeadlineDays()), null);
        repository.insertSource(source, event.occurredAt().toLocalDateTime());
        repository.changeCount(source.statisticDate(), source.assignedDepartmentId(), source.categoryCode(),
                source.currentStatus(), 1, event.occurredAt().toLocalDateTime());
        markProcessed(event);
    }

    @Transactional
    public void handleStatusChanged(EventEnvelope<ComplaintStatusChangedPayload> event) {
        if (isProcessed(event)) {
            return;
        }
        var payload = event.payload();
        ComplaintStatisticSource source = repository.findSource(payload.complaintId())
                .orElseThrow(() -> new IllegalStateException(
                        "Complaint statistic source not found: " + payload.complaintId()));
        long newDepartmentId = payload.assignedDepartmentId() == null
                ? source.assignedDepartmentId() : payload.assignedDepartmentId();
        if (source.currentStatus().equals(payload.currentStatus())
                && source.assignedDepartmentId().equals(newDepartmentId)) {
            markProcessed(event);
            return;
        }

        var changedAt = event.occurredAt().toLocalDateTime();
        repository.changeCount(source.statisticDate(), source.assignedDepartmentId(), source.categoryCode(),
                source.currentStatus(), -1, changedAt);
        repository.changeCount(source.statisticDate(), newDepartmentId, source.categoryCode(),
                payload.currentStatus(), 1, changedAt);
        var completedAt = "COMPLETED".equals(payload.currentStatus())
                ? payload.statusChangedAt().toLocalDateTime() : source.completedAt();
        repository.updateSource(payload.complaintId(), newDepartmentId, payload.currentStatus(),
                completedAt, changedAt);
        markProcessed(event);
    }

    private boolean isProcessed(EventEnvelope<?> event) {
        return processedEventRepository.existsById(event.eventId().toString());
    }

    private void markProcessed(EventEnvelope<?> event) {
        processedEventRepository.save(new ProcessedEvent(
                event.eventId(), event.eventType(), event.occurredAt().toLocalDateTime()));
    }
}
