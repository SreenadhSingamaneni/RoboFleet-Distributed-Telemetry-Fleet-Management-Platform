package com.roboverse.fleet.application.service;

import com.roboverse.fleet.application.exception.ResourceNotFoundException;
import com.roboverse.fleet.application.port.out.AlertEventPublisherPort;
import com.roboverse.fleet.application.port.out.AlertRepositoryPort;
import com.roboverse.fleet.domain.model.Alert;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertStatus;
import com.roboverse.fleet.domain.model.PageSlice;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService {
    private final AlertRepositoryPort alerts;
    private final AlertEventPublisherPort alertEvents;
    private final Clock clock;

    public AlertService(
            AlertRepositoryPort alerts,
            AlertEventPublisherPort alertEvents,
            Clock clock) {
        this.alerts = alerts;
        this.alertEvents = alertEvents;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageSlice<Alert> search(
            AlertStatus status, AlertSeverity severity, String robotId, int page, int size) {
        return alerts.search(status, severity, robotId, page, Math.min(size, 200));
    }

    @Transactional
    public Alert acknowledge(UUID id, String operator) {
        Alert alert = alerts.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + id));
        alert.acknowledge(operator, Instant.now(clock));
        Alert saved = alerts.save(alert);
        alertEvents.publish(List.of(saved));
        return saved;
    }
}
