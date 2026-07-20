package com.roboverse.fleet.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.roboverse.fleet.domain.model.Connectivity;
import com.roboverse.fleet.domain.model.MissionState;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = false)
public record TelemetryEvent(
        @NotNull UUID eventId,
        @Min(1) int schemaVersion,
        @NotNull @Pattern(regexp = "RBT-\\d{4}") String robotId,
        @Min(0) long sequenceNumber,
        @NotNull Instant recordedAt,
        @DecimalMin("-10000") @DecimalMax("10000") double x,
        @DecimalMin("-10000") @DecimalMax("10000") double y,
        @Min(-10) @Max(200) int floor,
        @DecimalMin("0") @DecimalMax("100") double batteryPercent,
        @DecimalMin("0") @DecimalMax("20") double speedMps,
        @DecimalMin("0") @DecimalMax(value = "360", inclusive = false) double headingDegrees,
        @DecimalMin("-40") @DecimalMax("150") double temperatureCelsius,
        @NotNull MissionState missionState,
        @NotNull Connectivity connectivity,
        @NotNull @Size(max = 20) List<@Pattern(regexp = "[A-Z0-9_]{1,64}") String> errorCodes) {

    public TelemetryPoint toDomain() {
        return new TelemetryPoint(eventId, robotId, sequenceNumber, recordedAt, x, y, floor,
                batteryPercent, speedMps, headingDegrees, temperatureCelsius,
                missionState, connectivity, errorCodes);
    }
}

