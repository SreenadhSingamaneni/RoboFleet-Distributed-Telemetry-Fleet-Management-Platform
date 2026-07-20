package com.roboverse.fleet.domain.alert;

import com.roboverse.fleet.config.FleetProperties;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertType;
import com.roboverse.fleet.domain.model.MissionState;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class NavigationStalledRule implements TelemetryAlertRule {
    private final double stalledSpeedMps;

    public NavigationStalledRule(FleetProperties properties) {
        stalledSpeedMps = properties.alerting().stalledSpeedMps();
    }

    @Override
    public AlertType type() {
        return AlertType.NAVIGATION_STALLED;
    }

    @Override
    public Optional<AlertCandidate> evaluate(TelemetryPoint telemetry) {
        boolean explicitlyStalled = telemetry.errorCodes().contains("NAVIGATION_STALLED");
        boolean noProgress = telemetry.missionState() == MissionState.IN_PROGRESS
                && telemetry.speedMps() <= stalledSpeedMps;
        return explicitlyStalled || noProgress
                ? Optional.of(new AlertCandidate(AlertSeverity.WARNING,
                        "Robot is not progressing during an active mission"))
                : Optional.empty();
    }
}

