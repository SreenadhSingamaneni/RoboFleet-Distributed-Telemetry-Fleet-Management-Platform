package com.roboverse.fleet.application.port.out;

import com.roboverse.fleet.domain.model.Alert;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertStatus;
import com.roboverse.fleet.domain.model.PageSlice;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface AlertRepositoryPort {
    List<Alert> findActiveByRobotIds(Set<String> robotIds);
    Optional<Alert> findById(UUID id);
    List<Alert> saveAll(Collection<Alert> alerts);
    Alert save(Alert alert);
    PageSlice<Alert> search(AlertStatus status, AlertSeverity severity, String robotId, int page, int size);
    long countOpen();
    long countOpenCritical();
}

