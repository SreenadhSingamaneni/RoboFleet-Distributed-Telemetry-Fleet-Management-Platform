package com.roboverse.fleet.application.port.out;

import com.roboverse.fleet.domain.model.Alert;
import java.util.List;

public interface AlertEventPublisherPort {
    void publish(List<Alert> alerts);
}

