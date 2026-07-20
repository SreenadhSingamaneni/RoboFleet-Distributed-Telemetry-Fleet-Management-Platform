package com.roboverse.fleet.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roboverse.fleet.config.FleetProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OutboxRelay {
    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
    private static final String CLAIM = """
            SELECT id, partition_key, payload::text
              FROM outbox_events
             WHERE published_at IS NULL
             ORDER BY occurred_at
             FOR UPDATE SKIP LOCKED
             LIMIT 100
            """;
    private static final String MARK_PUBLISHED = """
            UPDATE outbox_events
               SET published_at = now(), attempts = attempts + 1
             WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final String alertTopic;
    private final Counter published;
    private final Counter failed;

    public OutboxRelay(
            JdbcTemplate jdbcTemplate,
            KafkaTemplate<Object, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            FleetProperties properties,
            MeterRegistry registry) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        alertTopic = properties.topics().alerts();
        published = Counter.builder("fleet.outbox.published").register(registry);
        failed = Counter.builder("fleet.outbox.failures").register(registry);
    }

    @Scheduled(fixedDelayString = "${OUTBOX_RELAY_DELAY_MS:500}")
    public void relay() {
        try {
            transactionTemplate.executeWithoutResult(ignored -> relayTransaction());
        } catch (RuntimeException exception) {
            failed.increment();
            log.error("Outbox relay attempt failed; events remain unpublished", exception);
        }
    }

    @Scheduled(cron = "${OUTBOX_CLEANUP_CRON:0 15 3 * * *}")
    public void deletePublishedHistory() {
        jdbcTemplate.update("""
                DELETE FROM outbox_events
                 WHERE published_at < now() - interval '7 days'
                """);
    }

    private void relayTransaction() {
        List<PendingEvent> events = jdbcTemplate.query(CLAIM, (resultSet, rowNumber) ->
                new PendingEvent(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("partition_key"),
                        resultSet.getString("payload")));
        for (PendingEvent event : events) {
            try {
                AlertEvent payload = objectMapper.readValue(event.payload(), AlertEvent.class);
                kafkaTemplate.send(alertTopic, event.partitionKey(), payload).get(5, TimeUnit.SECONDS);
                jdbcTemplate.update(MARK_PUBLISHED, event.id());
                published.increment();
            } catch (Exception exception) {
                throw new IllegalStateException("Could not relay outbox event " + event.id(), exception);
            }
        }
    }

    private record PendingEvent(UUID id, String partitionKey, String payload) {}
}
