package com.roboverse.fleet.infrastructure.messaging;

import com.roboverse.fleet.application.service.TelemetryIngestionService;
import com.roboverse.fleet.config.FleetProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class TelemetryKafkaConsumer {
    private final TelemetryIngestionService ingestionService;
    private final Validator validator;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final FleetProperties properties;
    private final Clock clock;
    private final Counter rejectedCounter;

    public TelemetryKafkaConsumer(
            TelemetryIngestionService ingestionService,
            Validator validator,
            KafkaTemplate<Object, Object> kafkaTemplate,
            FleetProperties properties,
            Clock clock,
            MeterRegistry registry) {
        this.ingestionService = ingestionService;
        this.validator = validator;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.clock = clock;
        rejectedCounter = Counter.builder("fleet.telemetry.rejected").register(registry);
    }

    @KafkaListener(
            topics = "${fleet.topics.telemetry}",
            containerFactory = "telemetryKafkaListenerContainerFactory",
            concurrency = "${TELEMETRY_CONSUMER_CONCURRENCY:4}")
    public void consume(List<ConsumerRecord<String, TelemetryEvent>> records, Acknowledgment acknowledgment) {
        List<com.roboverse.fleet.domain.model.TelemetryPoint> valid = new ArrayList<>(records.size());
        List<CompletableFuture<?>> invalidPublishes = new ArrayList<>();
        for (ConsumerRecord<String, TelemetryEvent> record : records) {
            TelemetryEvent event = record.value();
            String rejection = rejectionReason(event);
            if (rejection == null) {
                valid.add(event.toDomain());
            } else {
                rejectedCounter.increment();
                invalidPublishes.add(kafkaTemplate.send(
                        properties.topics().telemetryInvalid(),
                        event.robotId(),
                        new InvalidTelemetryEvent(event, rejection, Instant.now(clock))));
            }
        }
        if (!valid.isEmpty()) {
            ingestionService.ingest(valid);
        }
        CompletableFuture.allOf(invalidPublishes.toArray(CompletableFuture[]::new)).join();
        acknowledgment.acknowledge();
    }

    private String rejectionReason(TelemetryEvent event) {
        if (event == null) return "Payload must not be null";
        var violations = validator.validate(event);
        if (!violations.isEmpty()) {
            return violations.stream()
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .sorted()
                    .collect(Collectors.joining("; "));
        }
        if (event.schemaVersion() != 1) return "Unsupported schemaVersion: " + event.schemaVersion();
        if (event.recordedAt().isAfter(Instant.now(clock).plus(properties.ingestion().maxClockSkew()))) {
            return "recordedAt exceeds allowed clock skew";
        }
        return null;
    }
}
