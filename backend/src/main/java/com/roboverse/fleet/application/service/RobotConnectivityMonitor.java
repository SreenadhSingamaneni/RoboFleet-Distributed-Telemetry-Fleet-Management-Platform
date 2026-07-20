package com.roboverse.fleet.application.service;

import com.roboverse.fleet.application.port.out.AlertEventPublisherPort;
import com.roboverse.fleet.application.port.out.AlertRepositoryPort;
import com.roboverse.fleet.application.port.out.ClusterLockPort;
import com.roboverse.fleet.application.port.out.RobotRepositoryPort;
import com.roboverse.fleet.config.FleetProperties;
import com.roboverse.fleet.domain.model.Alert;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertType;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RobotConnectivityMonitor {
    private final RobotRepositoryPort robots;
    private final AlertRepositoryPort alerts;
    private final AlertEventPublisherPort alertEvents;
    private final ClusterLockPort clusterLock;
    private final FleetProperties properties;
    private final Clock clock;

    public RobotConnectivityMonitor(
            RobotRepositoryPort robots,
            AlertRepositoryPort alerts,
            AlertEventPublisherPort alertEvents,
            ClusterLockPort clusterLock,
            FleetProperties properties,
            Clock clock) {
        this.robots = robots;
        this.alerts = alerts;
        this.alertEvents = alertEvents;
        this.clusterLock = clusterLock;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${fleet.alerting.stale-after:30s}")
    @Transactional
    public void markStaleRobotsOffline() {
        if (!clusterLock.tryAcquire("robot-connectivity-monitor")) {
            return;
        }
        Instant now = Instant.now(clock);
        Instant cutoff = now.minus(properties.alerting().staleAfter());
        var staleRobots = robots.findStaleActiveRobots(cutoff, 2_000);
        if (staleRobots.isEmpty()) {
            return;
        }

        var robotIds = staleRobots.stream().map(robot -> robot.id()).collect(Collectors.toSet());
        var alreadyAlerted = alerts.findActiveByRobotIds(robotIds).stream()
                .filter(alert -> alert.type() == AlertType.CONNECTIVITY_LOST)
                .map(Alert::robotId)
                .collect(Collectors.toSet());

        List<Alert> opened = new ArrayList<>();
        for (var robot : staleRobots) {
            if (!alreadyAlerted.contains(robot.id())) {
                opened.add(Alert.open(robot.id(), AlertType.CONNECTIVITY_LOST,
                        AlertSeverity.CRITICAL, "No telemetry received before stale deadline", now));
            }
        }
        robots.markOffline(staleRobots.stream().map(robot -> robot.id()).toList(), now);
        if (!opened.isEmpty()) {
            List<Alert> saved = alerts.saveAll(opened);
            alertEvents.publish(saved);
        }
    }
}
