package com.roboverse.fleet.infrastructure.persistence.jpa;

import com.roboverse.fleet.domain.model.RobotOperationalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "robots")
public class RobotEntity {
    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String facility;

    @Column(nullable = false)
    private String ward;

    @Column(nullable = false)
    private int floor;

    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false)
    private RobotOperationalStatus operationalStatus;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "battery_percent")
    private Double batteryPercent;

    @Column(name = "current_x")
    private Double currentX;

    @Column(name = "current_y")
    private Double currentY;

    @Version
    private long version;

    protected RobotEntity() {}

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getModel() { return model; }
    public String getFacility() { return facility; }
    public String getWard() { return ward; }
    public int getFloor() { return floor; }
    public RobotOperationalStatus getOperationalStatus() { return operationalStatus; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public Double getBatteryPercent() { return batteryPercent; }
    public Double getCurrentX() { return currentX; }
    public Double getCurrentY() { return currentY; }
}

