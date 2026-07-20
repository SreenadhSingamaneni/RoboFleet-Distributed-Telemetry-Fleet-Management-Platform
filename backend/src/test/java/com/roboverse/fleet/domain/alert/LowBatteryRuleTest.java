package com.roboverse.fleet.domain.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.roboverse.fleet.config.FleetProperties;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.Connectivity;
import com.roboverse.fleet.domain.model.MissionState;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LowBatteryRuleTest {
    private final LowBatteryRule rule = new LowBatteryRule(new FleetProperties(
            null, null, null, null,
            new FleetProperties.Alerting(20, 10, 70, 0.05, Duration.ofSeconds(30)),
            new FleetProperties.Ingestion(Duration.ofMinutes(5))));

    @Test
    void emitsCriticalAlertBelowCriticalThreshold() {
        var candidate = rule.evaluate(telemetry(7.5));

        assertThat(candidate).isPresent();
        assertThat(candidate.orElseThrow().severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void doesNotAlertAtHealthyBatteryLevel() {
        assertThat(rule.evaluate(telemetry(80))).isEmpty();
    }

    private TelemetryPoint telemetry(double battery) {
        return new TelemetryPoint(UUID.randomUUID(), "RBT-0001", 1, Instant.now(),
                1, 1, 1, battery, 0.5, 10, 40,
                MissionState.IN_PROGRESS, Connectivity.CONNECTED, List.of());
    }
}

