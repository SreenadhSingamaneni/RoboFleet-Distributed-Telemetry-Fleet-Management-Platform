package com.roboverse.fleet.infrastructure.persistence.jpa;

import com.roboverse.fleet.domain.model.RobotOperationalStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataRobotRepository
        extends JpaRepository<RobotEntity, String>, JpaSpecificationExecutor<RobotEntity> {

    long countByOperationalStatus(RobotOperationalStatus status);

    @Query("select coalesce(avg(r.batteryPercent), 0) from RobotEntity r where r.batteryPercent is not null")
    double averageBatteryPercent();

    @Query("""
            select r from RobotEntity r
            where r.operationalStatus in :statuses
              and r.lastSeenAt is not null
              and r.lastSeenAt < :cutoff
            order by r.lastSeenAt asc
            """)
    List<RobotEntity> findStale(
            @Param("statuses") List<RobotOperationalStatus> statuses,
            @Param("cutoff") Instant cutoff,
            Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE robots
               SET operational_status = 'OFFLINE', updated_at = :changedAt, version = version + 1
             WHERE id IN (:ids)
            """, nativeQuery = true)
    int markOffline(@Param("ids") List<String> ids, @Param("changedAt") Instant changedAt);
}
