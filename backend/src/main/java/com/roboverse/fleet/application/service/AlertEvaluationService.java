package com.roboverse.fleet.application.service;

import com.roboverse.fleet.application.port.out.AlertEventPublisherPort;
import com.roboverse.fleet.application.port.out.AlertRepositoryPort;
import com.roboverse.fleet.domain.alert.TelemetryAlertRule;
import com.roboverse.fleet.domain.model.Alert;
import com.roboverse.fleet.domain.model.AlertType;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertEvaluationService {
    private static final Duration RETRIGGER_COOLDOWN = Duration.ofSeconds(30);

    private final List<TelemetryAlertRule> rules;
    private final AlertRepositoryPort repository;
    private final AlertEventPublisherPort alertEvents;

    public AlertEvaluationService(
            List<TelemetryAlertRule> rules,
            AlertRepositoryPort repository,
            AlertEventPublisherPort alertEvents) {
        this.rules = List.copyOf(rules);
        this.repository = repository;
        this.alertEvents = alertEvents;
    }

    @Transactional
    public List<Alert> evaluateBatch(List<TelemetryPoint> telemetryBatch) {
        if (telemetryBatch.isEmpty()) {
            return List.of();
        }

        Set<String> robotIds = telemetryBatch.stream()
                .map(TelemetryPoint::robotId)
                .collect(java.util.stream.Collectors.toSet());
        Map<AlertKey, Alert> active = new HashMap<>();
        repository.findActiveByRobotIds(robotIds)
                .forEach(alert -> active.put(new AlertKey(alert.robotId(), alert.type()), alert));

        Set<Alert> changed = new LinkedHashSet<>();
        for (TelemetryPoint telemetry : telemetryBatch) {
            for (TelemetryAlertRule rule : rules) {
                AlertKey key = new AlertKey(telemetry.robotId(), rule.type());
                Alert existing = active.get(key);
                var candidate = rule.evaluate(telemetry);

                if (candidate.isPresent() && existing == null) {
                    var value = candidate.get();
                    Alert opened = Alert.open(telemetry.robotId(), rule.type(), value.severity(),
                            value.message(), telemetry.recordedAt());
                    active.put(key, opened);
                    changed.add(opened);
                } else if (candidate.isPresent()) {
                    var value = candidate.get();
                    if (existing.retrigger(value.severity(), value.message(), telemetry.recordedAt(),
                            RETRIGGER_COOLDOWN)) {
                        changed.add(existing);
                    }
                } else if (existing != null) {
                    existing.resolve(telemetry.recordedAt());
                    active.remove(key);
                    changed.add(existing);
                }
            }
        }

        if (changed.isEmpty()) {
            return List.of();
        }
        List<Alert> saved = repository.saveAll(new ArrayList<>(changed));
        alertEvents.publish(saved);
        return saved;
    }

    private record AlertKey(String robotId, AlertType type) {}
}
