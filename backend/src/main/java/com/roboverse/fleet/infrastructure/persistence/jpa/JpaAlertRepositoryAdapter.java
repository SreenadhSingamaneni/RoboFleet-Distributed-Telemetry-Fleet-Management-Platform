package com.roboverse.fleet.infrastructure.persistence.jpa;

import com.roboverse.fleet.application.port.out.AlertRepositoryPort;
import com.roboverse.fleet.domain.model.Alert;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertStatus;
import com.roboverse.fleet.domain.model.PageSlice;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAlertRepositoryAdapter implements AlertRepositoryPort {
    private static final List<AlertStatus> ACTIVE = List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);
    private final SpringDataAlertRepository repository;

    public JpaAlertRepositoryAdapter(SpringDataAlertRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Alert> findActiveByRobotIds(Set<String> robotIds) {
        if (robotIds.isEmpty()) {
            return List.of();
        }
        return repository.findActiveByRobotIds(robotIds).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Alert> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Alert> saveAll(Collection<Alert> alerts) {
        return repository.saveAll(alerts.stream().map(this::toEntity).toList())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Alert save(Alert alert) {
        return toDomain(repository.save(toEntity(alert)));
    }

    @Override
    public PageSlice<Alert> search(
            AlertStatus status, AlertSeverity severity, String robotId, int page, int size) {
        Specification<AlertEntity> specification = (root, ignored, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (severity != null) predicates.add(builder.equal(root.get("severity"), severity));
            if (robotId != null && !robotId.isBlank()) predicates.add(builder.equal(root.get("robotId"), robotId));
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        var result = repository.findAll(specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastTriggeredAt")));
        return new PageSlice<>(result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public long countOpen() {
        return repository.countByStatusIn(ACTIVE);
    }

    @Override
    public long countOpenCritical() {
        return repository.countByStatusInAndSeverity(ACTIVE, AlertSeverity.CRITICAL);
    }

    private AlertEntity toEntity(Alert alert) {
        return new AlertEntity(alert.id(), alert.robotId(), alert.type(), alert.severity(),
                alert.status(), alert.message(), alert.firstTriggeredAt(), alert.lastTriggeredAt(),
                alert.acknowledgedAt(), alert.acknowledgedBy(), alert.resolvedAt(),
                alert.occurrences(), alert.version());
    }

    private Alert toDomain(AlertEntity entity) {
        return Alert.rehydrate(entity.getId(), entity.getRobotId(), entity.getType(),
                entity.getSeverity(), entity.getStatus(), entity.getMessage(),
                entity.getFirstTriggeredAt(), entity.getLastTriggeredAt(), entity.getAcknowledgedAt(),
                entity.getAcknowledgedBy(), entity.getResolvedAt(), entity.getOccurrences(),
                entity.getVersion());
    }
}

