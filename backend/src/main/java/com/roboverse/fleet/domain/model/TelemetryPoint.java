package com.roboverse.fleet.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TelemetryPoint(
        UUID eventId,
        String robotId,
        long sequenceNumber,
        Instant recordedAt,
        double x,
        double y,
        int floor,
        double batteryPercent,
        double speedMps,
        double headingDegrees,
        double temperatureCelsius,
        MissionState missionState,
        Connectivity connectivity,
        List<String> errorCodes) {

    public TelemetryPoint {
        errorCodes = errorCodes == null ? List.of() : List.copyOf(errorCodes);
    }
}

