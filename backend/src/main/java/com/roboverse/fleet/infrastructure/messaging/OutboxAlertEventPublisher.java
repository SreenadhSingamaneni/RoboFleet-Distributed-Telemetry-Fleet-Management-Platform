package com.roboverse.fleet.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roboverse.fleet.application.port.out.AlertEventPublisherPort;
import com.roboverse.fleet.domain.model.Alert;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.UUID;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OutboxAlertEventPublisher implements AlertEventPublisherPort {
    private static final String INSERT = """
            INSERT INTO outbox_events (
                id, aggregate_type, aggregate_id, event_type, partition_key, payload, occurred_at)
            VALUES (?, 'ALERT', ?, 'ALERT_CHANGED', ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OutboxAlertEventPublisher(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(List<Alert> alerts) {
        if (alerts.isEmpty()) return;
        jdbcTemplate.batchUpdate(INSERT, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                Alert alert = alerts.get(index);
                statement.setObject(1, UUID.randomUUID());
                statement.setString(2, alert.id().toString());
                statement.setString(3, alert.robotId());
                statement.setObject(4, jsonb(AlertEvent.from(alert)), Types.OTHER);
                statement.setTimestamp(5, Timestamp.from(alert.lastTriggeredAt()));
            }

            @Override
            public int getBatchSize() {
                return alerts.size();
            }
        });
    }

    private PGobject jsonb(AlertEvent event) throws SQLException {
        try {
            PGobject json = new PGobject();
            json.setType("jsonb");
            json.setValue(objectMapper.writeValueAsString(event));
            return json;
        } catch (JsonProcessingException exception) {
            throw new SQLException("Could not serialize alert outbox event", exception);
        }
    }
}

