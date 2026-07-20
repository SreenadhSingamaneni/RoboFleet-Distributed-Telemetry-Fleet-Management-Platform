package com.roboverse.fleet.infrastructure.persistence.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.roboverse.fleet.domain.model.Connectivity;
import com.roboverse.fleet.domain.model.MissionState;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class TelemetryJdbcStoreTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    static TelemetryJdbcStore store;

    @BeforeAll
    static void setUpDatabase() {
        var dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).load().migrate();
        store = new TelemetryJdbcStore(new JdbcTemplate(dataSource),
                new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void persistsOnceAndSuppressesDuplicateEventId() {
        Instant now = Instant.now();
        TelemetryPoint point = new TelemetryPoint(UUID.randomUUID(), "RBT-0001", 1, now,
                12, 23, 2, 62, 0.8, 180, 43,
                MissionState.IN_PROGRESS, Connectivity.CONNECTED, List.of());

        assertThat(store.saveBatch(List.of(point))).containsExactly(point);
        assertThat(store.saveBatch(List.of(point))).isEmpty();
        assertThat(store.findHistory("RBT-0001", now.minusSeconds(5), now.plusSeconds(5), 10))
                .hasSize(1)
                .first()
                .extracting(TelemetryPoint::eventId)
                .isEqualTo(point.eventId());
    }
}

