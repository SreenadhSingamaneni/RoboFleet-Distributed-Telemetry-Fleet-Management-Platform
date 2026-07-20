package com.roboverse.fleet.infrastructure.persistence.jpa;

import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertStatus;
import com.roboverse.fleet.domain.model.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class AlertEntity {
    @Id
    private UUID id;

    @Column(name = "robot_id", nullable = false, length = 32)
    private String robotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AlertStatus status;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "first_triggered_at", nullable = false)
    private Instant firstTriggeredAt;

    @Column(name = "last_triggered_at", nullable = false)
    private Instant lastTriggeredAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(nullable = false)
    private int occurrences;

    @Version
    private long version;

    protected AlertEntity() {}

    public AlertEntity(
            UUID id, String robotId, AlertType type, AlertSeverity severity, AlertStatus status,
            String message, Instant firstTriggeredAt, Instant lastTriggeredAt,
            Instant acknowledgedAt, String acknowledgedBy, Instant resolvedAt,
            int occurrences, long version) {
        this.id = id;
        this.robotId = robotId;
        this.type = type;
        this.severity = severity;
        this.status = status;
        this.message = message;
        this.firstTriggeredAt = firstTriggeredAt;
        this.lastTriggeredAt = lastTriggeredAt;
        this.acknowledgedAt = acknowledgedAt;
        this.acknowledgedBy = acknowledgedBy;
        this.resolvedAt = resolvedAt;
        this.occurrences = occurrences;
        this.version = version;
    }

    public UUID getId() { return id; }
    public String getRobotId() { return robotId; }
    public AlertType getType() { return type; }
    public AlertSeverity getSeverity() { return severity; }
    public AlertStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public Instant getFirstTriggeredAt() { return firstTriggeredAt; }
    public Instant getLastTriggeredAt() { return lastTriggeredAt; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public String getAcknowledgedBy() { return acknowledgedBy; }
    public Instant getResolvedAt() { return resolvedAt; }
    public int getOccurrences() { return occurrences; }
    public long getVersion() { return version; }
}

