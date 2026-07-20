package com.roboverse.fleet.infrastructure.persistence.jpa;

import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataAlertRepository
        extends JpaRepository<AlertEntity, UUID>, JpaSpecificationExecutor<AlertEntity> {

    @Query("""
            select a from AlertEntity a
             where a.robotId in :robotIds
               and a.status in (com.roboverse.fleet.domain.model.AlertStatus.OPEN,
                                com.roboverse.fleet.domain.model.AlertStatus.ACKNOWLEDGED)
            """)
    List<AlertEntity> findActiveByRobotIds(@Param("robotIds") Set<String> robotIds);

    long countByStatusIn(List<AlertStatus> statuses);

    long countByStatusInAndSeverity(List<AlertStatus> statuses, AlertSeverity severity);
}

