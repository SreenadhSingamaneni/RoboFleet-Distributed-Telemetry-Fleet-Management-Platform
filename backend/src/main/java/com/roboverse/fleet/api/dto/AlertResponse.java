package com.roboverse.fleet.api.dto;

import com.roboverse.fleet.domain.model.Alert;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertStatus;
import com.roboverse.fleet.domain.model.AlertType;
import java.time.Instant;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        String robotId,
        AlertType type,
        AlertSeverity severity,
        AlertStatus status,
        String message,
        Instant firstTriggeredAt,
        Instant lastTriggeredAt,
        Instant acknowledgedAt,
        String acknowledgedBy,
        Instant resolvedAt,
        int occurrences) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(alert.id(), alert.robotId(), alert.type(), alert.severity(),
                alert.status(), alert.message(), alert.firstTriggeredAt(), alert.lastTriggeredAt(),
                alert.acknowledgedAt(), alert.acknowledgedBy(), alert.resolvedAt(), alert.occurrences());
    }
}

