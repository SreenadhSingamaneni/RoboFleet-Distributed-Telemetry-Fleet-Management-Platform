package com.roboverse.fleet.application.port.out;

import com.roboverse.fleet.domain.model.FleetSummary;
import com.roboverse.fleet.domain.model.PageSlice;
import com.roboverse.fleet.domain.model.Robot;
import com.roboverse.fleet.domain.model.RobotOperationalStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RobotRepositoryPort {
    Optional<Robot> findById(String id);
    PageSlice<Robot> search(String query, RobotOperationalStatus status, int page, int size);
    List<Robot> findStaleActiveRobots(Instant cutoff, int limit);
    void markOffline(List<String> robotIds, Instant changedAt);
    FleetSummary summarize(long openAlerts, long criticalAlerts);
}

