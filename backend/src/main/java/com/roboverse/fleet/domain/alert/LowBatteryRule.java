package com.roboverse.fleet.domain.alert;

import com.roboverse.fleet.config.FleetProperties;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertType;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LowBatteryRule implements TelemetryAlertRule {
    private final FleetProperties.Alerting thresholds;

    public LowBatteryRule(FleetProperties properties) {
        this.thresholds = properties.alerting();
    }

    @Override
    public AlertType type() {
        return AlertType.LOW_BATTERY;
    }

    @Override
    public Optional<AlertCandidate> evaluate(TelemetryPoint telemetry) {
        if (telemetry.batteryPercent() >= thresholds.lowBatteryWarningPercent()) {
            return Optional.empty();
        }
        AlertSeverity severity = telemetry.batteryPercent() < thresholds.lowBatteryCriticalPercent()
                ? AlertSeverity.CRITICAL
                : AlertSeverity.WARNING;
        return Optional.of(new AlertCandidate(severity,
                "Battery at %.1f%%".formatted(telemetry.batteryPercent())));
    }
}

