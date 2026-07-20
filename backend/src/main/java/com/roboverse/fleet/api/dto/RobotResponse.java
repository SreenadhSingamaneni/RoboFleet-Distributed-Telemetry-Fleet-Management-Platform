package com.roboverse.fleet.api.dto;

import com.roboverse.fleet.domain.model.Robot;
import com.roboverse.fleet.domain.model.RobotOperationalStatus;
import java.time.Instant;

public record RobotResponse(
        String id,
        String displayName,
        String model,
        String facility,
        String ward,
        int floor,
        RobotOperationalStatus operationalStatus,
        Instant lastSeenAt,
        Double batteryPercent,
        Double currentX,
        Double currentY) {

    public static RobotResponse from(Robot robot) {
        return new RobotResponse(robot.id(), robot.displayName(), robot.model(), robot.facility(),
                robot.ward(), robot.floor(), robot.operationalStatus(), robot.lastSeenAt(),
                robot.batteryPercent(), robot.currentX(), robot.currentY());
    }
}

