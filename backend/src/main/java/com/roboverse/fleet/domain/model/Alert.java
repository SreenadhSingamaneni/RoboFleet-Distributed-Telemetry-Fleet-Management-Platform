package com.roboverse.fleet.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Alert {
    private final UUID id;
    private final String robotId;
    private final AlertType type;
    private AlertSeverity severity;
    private AlertStatus status;
    private String message;
    private final Instant firstTriggeredAt;
    private Instant lastTriggeredAt;
    private Instant acknowledgedAt;
    private String acknowledgedBy;
    private Instant resolvedAt;
    private int occurrences;
    private final long version;

    private Alert(
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
            int occurrences,
            long version) {
        this.id = Objects.requireNonNull(id);
        this.robotId = Objects.requireNonNull(robotId);
        this.type = Objects.requireNonNull(type);
        this.severity = Objects.requireNonNull(severity);
        this.status = Objects.requireNonNull(status);
        this.message = Objects.requireNonNull(message);
        this.firstTriggeredAt = Objects.requireNonNull(firstTriggeredAt);
        this.lastTriggeredAt = Objects.requireNonNull(lastTriggeredAt);
        this.acknowledgedAt = acknowledgedAt;
        this.acknowledgedBy = acknowledgedBy;
        this.resolvedAt = resolvedAt;
        this.occurrences = occurrences;
        this.version = version;
    }

    public static Alert open(
            String robotId,
            AlertType type,
            AlertSeverity severity,
            String message,
            Instant occurredAt) {
        return new Alert(UUID.randomUUID(), robotId, type, severity, AlertStatus.OPEN,
                message, occurredAt, occurredAt, null, null, null, 1, 0);
    }

    public static Alert rehydrate(
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
            int occurrences,
            long version) {
        return new Alert(id, robotId, type, severity, status, message, firstTriggeredAt,
                lastTriggeredAt, acknowledgedAt, acknowledgedBy, resolvedAt, occurrences, version);
    }

    public boolean retrigger(AlertSeverity newSeverity, String newMessage, Instant occurredAt, Duration cooldown) {
        boolean severityChanged = newSeverity != severity;
        boolean cooldownElapsed = !occurredAt.isBefore(lastTriggeredAt.plus(cooldown));
        if (!severityChanged && !cooldownElapsed) {
            return false;
        }
        severity = newSeverity;
        message = newMessage;
        lastTriggeredAt = occurredAt;
        occurrences++;
        return true;
    }

    public void acknowledge(String operator, Instant acknowledgedAt) {
        if (status == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Resolved alerts cannot be acknowledged");
        }
        status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = Objects.requireNonNull(acknowledgedAt);
        acknowledgedBy = requireText(operator, "operator");
    }

    public void resolve(Instant resolvedAt) {
        if (status != AlertStatus.RESOLVED) {
            status = AlertStatus.RESOLVED;
            this.resolvedAt = Objects.requireNonNull(resolvedAt);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    public UUID id() { return id; }
    public String robotId() { return robotId; }
    public AlertType type() { return type; }
    public AlertSeverity severity() { return severity; }
    public AlertStatus status() { return status; }
    public String message() { return message; }
    public Instant firstTriggeredAt() { return firstTriggeredAt; }
    public Instant lastTriggeredAt() { return lastTriggeredAt; }
    public Instant acknowledgedAt() { return acknowledgedAt; }
    public String acknowledgedBy() { return acknowledgedBy; }
    public Instant resolvedAt() { return resolvedAt; }
    public int occurrences() { return occurrences; }
    public long version() { return version; }
}
