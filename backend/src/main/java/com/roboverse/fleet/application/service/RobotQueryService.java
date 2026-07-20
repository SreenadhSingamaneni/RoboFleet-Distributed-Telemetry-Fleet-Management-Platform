package com.roboverse.fleet.application.service;

import com.roboverse.fleet.application.exception.ResourceNotFoundException;
import com.roboverse.fleet.application.port.out.LatestTelemetryCachePort;
import com.roboverse.fleet.application.port.out.RobotRepositoryPort;
import com.roboverse.fleet.application.port.out.TelemetryStorePort;
import com.roboverse.fleet.domain.model.PageSlice;
import com.roboverse.fleet.domain.model.Robot;
import com.roboverse.fleet.domain.model.RobotOperationalStatus;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RobotQueryService {
    private final RobotRepositoryPort robots;
    private final TelemetryStorePort telemetryStore;
    private final LatestTelemetryCachePort telemetryCache;

    public RobotQueryService(
            RobotRepositoryPort robots,
            TelemetryStorePort telemetryStore,
            LatestTelemetryCachePort telemetryCache) {
        this.robots = robots;
        this.telemetryStore = telemetryStore;
        this.telemetryCache = telemetryCache;
    }

    public PageSlice<Robot> search(String query, RobotOperationalStatus status, int page, int size) {
        return robots.search(query, status, page, Math.min(size, 200));
    }

    public Robot get(String id) {
        return robots.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Robot not found: " + id));
    }

    public Optional<TelemetryPoint> latestTelemetry(String robotId) {
        get(robotId);
        return telemetryCache.get(robotId);
    }

    public List<TelemetryPoint> history(String robotId, Instant from, Instant to, int limit) {
        get(robotId);
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException("from must be before to");
        }
        return telemetryStore.findHistory(robotId, from, to, Math.min(limit, 10_000));
    }
}

