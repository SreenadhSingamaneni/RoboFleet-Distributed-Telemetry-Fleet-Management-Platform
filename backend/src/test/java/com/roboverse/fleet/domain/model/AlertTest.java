package com.roboverse.fleet.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AlertTest {
    private static final Instant NOW = Instant.parse("2026-07-19T12:00:00Z");

    @Test
    void enforcesAcknowledgmentAndResolutionStateTransitions() {
        Alert alert = Alert.open("RBT-0001", AlertType.LOW_BATTERY,
                AlertSeverity.WARNING, "Battery low", NOW);

        alert.acknowledge("operator@example.com", NOW.plusSeconds(5));
        alert.resolve(NOW.plusSeconds(20));

        assertThat(alert.status()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.acknowledgedBy()).isEqualTo("operator@example.com");
        assertThatThrownBy(() -> alert.acknowledge("operator", NOW.plusSeconds(30)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void suppressesIdenticalRetriggersInsideCooldown() {
        Alert alert = Alert.open("RBT-0001", AlertType.LOW_BATTERY,
                AlertSeverity.WARNING, "Battery at 19%", NOW);

        boolean changed = alert.retrigger(AlertSeverity.WARNING, "Battery at 18%",
                NOW.plusSeconds(10), Duration.ofSeconds(30));

        assertThat(changed).isFalse();
        assertThat(alert.occurrences()).isEqualTo(1);
    }

    @Test
    void immediately_records_severity_escalation() {
        Alert alert = Alert.open("RBT-0001", AlertType.LOW_BATTERY,
                AlertSeverity.WARNING, "Battery at 19%", NOW);

        boolean changed = alert.retrigger(AlertSeverity.CRITICAL, "Battery at 8%",
                NOW.plusSeconds(10), Duration.ofSeconds(30));

        assertThat(changed).isTrue();
        assertThat(alert.severity()).isEqualTo(AlertSeverity.CRITICAL);
        assertThat(alert.occurrences()).isEqualTo(2);
    }
}

