package com.roboverse.fleet.domain.alert;

import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertType;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RobotErrorRule implements TelemetryAlertRule {
    @Override
    public AlertType type() {
        return AlertType.ROBOT_ERROR;
    }

    @Override
    public Optional<AlertCandidate> evaluate(TelemetryPoint telemetry) {
        var actionableErrors = telemetry.errorCodes().stream()
                .filter(code -> !code.equals("NAVIGATION_STALLED"))
                .toList();
        return actionableErrors.isEmpty()
                ? Optional.empty()
                : Optional.of(new AlertCandidate(AlertSeverity.WARNING,
                        "Robot reported: " + String.join(", ", actionableErrors)));
    }
}

