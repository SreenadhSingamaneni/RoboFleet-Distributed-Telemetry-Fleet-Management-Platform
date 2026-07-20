package com.roboverse.fleet.domain.alert;

import com.roboverse.fleet.domain.model.AlertType;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.util.Optional;

public interface TelemetryAlertRule {
    AlertType type();
    Optional<AlertCandidate> evaluate(TelemetryPoint telemetry);
}

