package com.roboverse.fleet.domain.model;

public record FleetSummary(
        long totalRobots,
        long onlineRobots,
        long degradedRobots,
        long offlineRobots,
        long maintenanceRobots,
        long openAlerts,
        long criticalAlerts,
        double averageBatteryPercent) {}

