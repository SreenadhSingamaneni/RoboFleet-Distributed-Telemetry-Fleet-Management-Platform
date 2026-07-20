package com.roboverse.fleet.api.dto;

import com.roboverse.fleet.domain.model.Connectivity;
import com.roboverse.fleet.domain.model.MissionState;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TelemetryResponse(
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

    public static TelemetryResponse from(TelemetryPoint point) {
        return new TelemetryResponse(point.eventId(), point.robotId(), point.sequenceNumber(),
                point.recordedAt(), point.x(), point.y(), point.floor(), point.batteryPercent(),
                point.speedMps(), point.headingDegrees(), point.temperatureCelsius(),
                point.missionState(), point.connectivity(), point.errorCodes());
    }
}

