package com.roboverse.fleet.application.port.out;

import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.time.Instant;
import java.util.List;

public interface TelemetryStorePort {
    List<TelemetryPoint> saveBatch(List<TelemetryPoint> telemetry);
    List<TelemetryPoint> findHistory(String robotId, Instant from, Instant to, int limit);
}

