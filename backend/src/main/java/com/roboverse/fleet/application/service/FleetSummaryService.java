package com.roboverse.fleet.application.service;

import com.roboverse.fleet.application.port.out.AlertRepositoryPort;
import com.roboverse.fleet.application.port.out.RobotRepositoryPort;
import com.roboverse.fleet.domain.model.FleetSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FleetSummaryService {
    private final RobotRepositoryPort robots;
    private final AlertRepositoryPort alerts;

    public FleetSummaryService(RobotRepositoryPort robots, AlertRepositoryPort alerts) {
        this.robots = robots;
        this.alerts = alerts;
    }

    public FleetSummary getSummary() {
        return robots.summarize(alerts.countOpen(), alerts.countOpenCritical());
    }
}

