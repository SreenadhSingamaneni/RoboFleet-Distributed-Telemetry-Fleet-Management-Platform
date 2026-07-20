package com.roboverse.fleet.infrastructure.persistence.jpa;

import com.roboverse.fleet.application.port.out.RobotRepositoryPort;
import com.roboverse.fleet.domain.model.FleetSummary;
import com.roboverse.fleet.domain.model.PageSlice;
import com.roboverse.fleet.domain.model.Robot;
import com.roboverse.fleet.domain.model.RobotOperationalStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class JpaRobotRepositoryAdapter implements RobotRepositoryPort {
    private final SpringDataRobotRepository repository;

    public JpaRobotRepositoryAdapter(SpringDataRobotRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Robot> findById(String id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public PageSlice<Robot> search(
            String query, RobotOperationalStatus status, int page, int size) {
        Specification<RobotEntity> specification = (root, ignored, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("id")), pattern),
                        builder.like(builder.lower(root.get("displayName")), pattern),
                        builder.like(builder.lower(root.get("ward")), pattern)));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("operationalStatus"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("lastSeenAt"), Sort.Order.asc("id")));
        var result = repository.findAll(specification, pageable);
        return new PageSlice<>(result.getContent().stream().map(this::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    public List<Robot> findStaleActiveRobots(Instant cutoff, int limit) {
        return repository.findStale(
                        List.of(RobotOperationalStatus.ONLINE, RobotOperationalStatus.DEGRADED),
                        cutoff,
                        PageRequest.of(0, limit))
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void markOffline(List<String> robotIds, Instant changedAt) {
        if (!robotIds.isEmpty()) {
            repository.markOffline(robotIds, changedAt);
        }
    }

    @Override
    public FleetSummary summarize(long openAlerts, long criticalAlerts) {
        return new FleetSummary(
                repository.count(),
                repository.countByOperationalStatus(RobotOperationalStatus.ONLINE),
                repository.countByOperationalStatus(RobotOperationalStatus.DEGRADED),
                repository.countByOperationalStatus(RobotOperationalStatus.OFFLINE),
                repository.countByOperationalStatus(RobotOperationalStatus.MAINTENANCE),
                openAlerts,
                criticalAlerts,
                repository.averageBatteryPercent());
    }

    private Robot toDomain(RobotEntity entity) {
        return new Robot(entity.getId(), entity.getDisplayName(), entity.getModel(),
                entity.getFacility(), entity.getWard(), entity.getFloor(),
                entity.getOperationalStatus(), entity.getLastSeenAt(), entity.getBatteryPercent(),
                entity.getCurrentX(), entity.getCurrentY());
    }
}
