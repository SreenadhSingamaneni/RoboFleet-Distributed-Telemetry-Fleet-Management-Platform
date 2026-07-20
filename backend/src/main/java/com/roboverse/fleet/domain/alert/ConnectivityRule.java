package com.roboverse.fleet.domain.alert;

import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertType;
import com.roboverse.fleet.domain.model.Connectivity;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ConnectivityRule implements TelemetryAlertRule {
    @Override
    public AlertType type() {
        return AlertType.CONNECTIVITY_LOST;
    }

    @Override
    public Optional<AlertCandidate> evaluate(TelemetryPoint telemetry) {
        return telemetry.connectivity() == Connectivity.CONNECTED
                ? Optional.empty()
                : Optional.of(new AlertCandidate(
                        telemetry.connectivity() == Connectivity.DISCONNECTED
                                ? AlertSeverity.CRITICAL
                                : AlertSeverity.WARNING,
                        "Connectivity state is " + telemetry.connectivity()));
    }
}

