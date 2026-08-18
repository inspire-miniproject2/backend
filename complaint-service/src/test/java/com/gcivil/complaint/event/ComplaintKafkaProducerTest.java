package com.gcivil.complaint.event;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComplaintKafkaProducerTest {
    @Test
    void sendsEnvelopeUsingComplaintIdAsKafkaKey() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        when(kafkaTemplate.send(any(String.class), any(String.class), any()))
                .thenReturn(new CompletableFuture<>());
        var producer = new ComplaintKafkaProducer(kafkaTemplate);
        var payload = new ComplaintCreatedPayload(
                1001L, "CIV-2026-000184", 501L, 10L, "ROAD", "RECEIVED",
                OffsetDateTime.parse("2026-08-15T09:30:00+09:00"), List.of(NotifyChannel.IN_APP));
        var message = new ComplaintEventMessage<>(
                UUID.fromString("4d46c02f-cce8-4afd-9cfa-14fceb56ae89"), "complaint.created.v1",
                ComplaintEventTypes.COMPLAINT_CREATED, payload.submittedAt(), "1001", payload);

        producer.sendAfterCommit(message);

        var envelopeCaptor = org.mockito.ArgumentCaptor.forClass(EventEnvelope.class);
        verify(kafkaTemplate).send(eq("complaint.created.v1"), eq("1001"), envelopeCaptor.capture());
        EventEnvelope<?> envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventId()).isEqualTo(message.eventId());
        assertThat(envelope.eventVersion()).isEqualTo("v1");
        assertThat(envelope.producer()).isEqualTo("complaint-service");
        assertThat(envelope.partitionKey()).isEqualTo("1001");
        assertThat(envelope.payload()).isEqualTo(payload);
    }

    @Test
    void listenerRunsOnlyAfterTransactionCommit() throws Exception {
        Method method = ComplaintKafkaProducer.class.getDeclaredMethod(
                "sendAfterCommit", ComplaintEventMessage.class);

        TransactionalEventListener listener = method.getAnnotation(TransactionalEventListener.class);

        assertThat(listener).isNotNull();
        assertThat(listener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(listener.fallbackExecution()).isFalse();
    }
}
