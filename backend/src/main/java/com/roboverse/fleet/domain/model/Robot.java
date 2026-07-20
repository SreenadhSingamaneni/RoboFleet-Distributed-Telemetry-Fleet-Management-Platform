package com.roboverse.fleet.domain.model;

import java.time.Instant;

public record Robot(
        String id,
        String displayName,
        String model,
        String facility,
        String ward,
        int floor,
        RobotOperationalStatus operationalStatus,
        Instant lastSeenAt,
        Double batteryPercent,
        Double currentX,
        Double currentY) {}

