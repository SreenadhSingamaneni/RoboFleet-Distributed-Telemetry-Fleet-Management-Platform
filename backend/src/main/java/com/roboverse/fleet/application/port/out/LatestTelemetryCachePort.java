package com.roboverse.fleet.application.port.out;

import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.util.List;
import java.util.Optional;

public interface LatestTelemetryCachePort {
    void putAll(List<TelemetryPoint> telemetry);
    Optional<TelemetryPoint> get(String robotId);
}

