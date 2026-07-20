package com.roboverse.fleet.infrastructure.persistence.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roboverse.fleet.application.port.out.TelemetryStorePort;
import com.roboverse.fleet.domain.model.Connectivity;
import com.roboverse.fleet.domain.model.MissionState;
import com.roboverse.fleet.domain.model.RobotOperationalStatus;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TelemetryJdbcStore implements TelemetryStorePort {
    private static final String INSERT_SQL = """
            INSERT INTO telemetry (
                event_id, robot_id, sequence_number, recorded_at, x, y, floor,
                battery_percent, speed_mps, heading_degrees, temperature_celsius,
                mission_state, connectivity, error_codes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (event_id, recorded_at) DO NOTHING
            """;

    private static final String UPDATE_ROBOT_SQL = """
            UPDATE robots
               SET operational_status = ?, last_seen_at = ?, battery_percent = ?,
                   current_x = ?, current_y = ?, floor = ?, updated_at = now(), version = version + 1
             WHERE id = ? AND (last_seen_at IS NULL OR last_seen_at <= ?)
            """;

    private static final String HISTORY_SQL = """
            SELECT event_id, robot_id, sequence_number, recorded_at, x, y, floor,
                   battery_percent, speed_mps, heading_degrees, temperature_celsius,
                   mission_state, connectivity, error_codes::text
              FROM telemetry
             WHERE robot_id = ? AND recorded_at >= ? AND recorded_at <= ?
             ORDER BY recorded_at DESC
             LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public TelemetryJdbcStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public List<TelemetryPoint> saveBatch(List<TelemetryPoint> telemetry) {
        if (telemetry.isEmpty()) {
            return List.of();
        }
        int[] counts = jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                TelemetryPoint point = telemetry.get(index);
                statement.setObject(1, point.eventId());
                statement.setString(2, point.robotId());
                statement.setLong(3, point.sequenceNumber());
                statement.setTimestamp(4, Timestamp.from(point.recordedAt()));
                statement.setDouble(5, point.x());
                statement.setDouble(6, point.y());
                statement.setInt(7, point.floor());
                statement.setDouble(8, point.batteryPercent());
                statement.setDouble(9, point.speedMps());
                statement.setDouble(10, point.headingDegrees());
                statement.setDouble(11, point.temperatureCelsius());
                statement.setString(12, point.missionState().name());
                statement.setString(13, point.connectivity().name());
                statement.setObject(14, jsonb(point.errorCodes()), Types.OTHER);
            }

            @Override
            public int getBatchSize() {
                return telemetry.size();
            }
        });

        List<TelemetryPoint> accepted = new ArrayList<>(telemetry.size());
        for (int i = 0; i < counts.length; i++) {
            if (counts[i] != 0) {
                accepted.add(telemetry.get(i));
            }
        }
        updateRobotSnapshots(accepted);
        return accepted;
    }

    @Override
    public List<TelemetryPoint> findHistory(String robotId, Instant from, Instant to, int limit) {
        return jdbcTemplate.query(HISTORY_SQL, (resultSet, rowNumber) -> new TelemetryPoint(
                        resultSet.getObject("event_id", UUID.class),
                        resultSet.getString("robot_id"),
                        resultSet.getLong("sequence_number"),
                        resultSet.getTimestamp("recorded_at").toInstant(),
                        resultSet.getDouble("x"),
                        resultSet.getDouble("y"),
                        resultSet.getInt("floor"),
                        resultSet.getDouble("battery_percent"),
                        resultSet.getDouble("speed_mps"),
                        resultSet.getDouble("heading_degrees"),
                        resultSet.getDouble("temperature_celsius"),
                        MissionState.valueOf(resultSet.getString("mission_state")),
                        Connectivity.valueOf(resultSet.getString("connectivity")),
                        readErrors(resultSet.getString("error_codes"))),
                robotId, Timestamp.from(from), Timestamp.from(to), limit);
    }

    private void updateRobotSnapshots(List<TelemetryPoint> accepted) {
        jdbcTemplate.batchUpdate(UPDATE_ROBOT_SQL, accepted, accepted.size(),
                (statement, point) -> {
                    statement.setString(1, statusFor(point).name());
                    statement.setTimestamp(2, Timestamp.from(point.recordedAt()));
                    statement.setDouble(3, point.batteryPercent());
                    statement.setDouble(4, point.x());
                    statement.setDouble(5, point.y());
                    statement.setInt(6, point.floor());
                    statement.setString(7, point.robotId());
                    statement.setTimestamp(8, Timestamp.from(point.recordedAt()));
                });
    }

    private RobotOperationalStatus statusFor(TelemetryPoint point) {
        return switch (point.connectivity()) {
            case CONNECTED -> point.errorCodes().isEmpty()
                    ? RobotOperationalStatus.ONLINE : RobotOperationalStatus.DEGRADED;
            case DEGRADED -> RobotOperationalStatus.DEGRADED;
            case DISCONNECTED -> RobotOperationalStatus.OFFLINE;
        };
    }

    private PGobject jsonb(List<String> errors) throws SQLException {
        try {
            PGobject json = new PGobject();
            json.setType("jsonb");
            json.setValue(objectMapper.writeValueAsString(errors));
            return json;
        } catch (JsonProcessingException exception) {
            throw new SQLException("Could not serialize telemetry error codes", exception);
        }
    }

    private List<String> readErrors(String json) throws SQLException {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new SQLException("Could not deserialize telemetry error codes", exception);
        }
    }
}
