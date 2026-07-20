package com.roboverse.fleet.domain.alert;

import com.roboverse.fleet.config.FleetProperties;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertType;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class HighTemperatureRule implements TelemetryAlertRule {
    private final double threshold;

    public HighTemperatureRule(FleetProperties properties) {
        threshold = properties.alerting().highTemperatureCelsius();
    }

    @Override
    public AlertType type() {
        return AlertType.HIGH_TEMPERATURE;
    }

    @Override
    public Optional<AlertCandidate> evaluate(TelemetryPoint telemetry) {
        if (telemetry.temperatureCelsius() <= threshold) {
            return Optional.empty();
        }
        return Optional.of(new AlertCandidate(AlertSeverity.CRITICAL,
                "Controller temperature at %.1f°C".formatted(telemetry.temperatureCelsius())));
    }
}

