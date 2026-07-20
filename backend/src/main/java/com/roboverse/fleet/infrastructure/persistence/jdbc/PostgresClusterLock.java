package com.roboverse.fleet.infrastructure.persistence.jdbc;

import com.roboverse.fleet.application.port.out.ClusterLockPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostgresClusterLock implements ClusterLockPort {
    private final JdbcTemplate jdbcTemplate;

    public PostgresClusterLock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean tryAcquire(String lockName) {
        Boolean acquired = jdbcTemplate.queryForObject(
                "SELECT pg_try_advisory_xact_lock(hashtext(?))",
                Boolean.class,
                lockName);
        return Boolean.TRUE.equals(acquired);
    }
}
