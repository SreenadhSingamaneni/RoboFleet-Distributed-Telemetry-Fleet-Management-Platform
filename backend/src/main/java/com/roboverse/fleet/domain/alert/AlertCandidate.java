package com.roboverse.fleet.domain.alert;

import com.roboverse.fleet.domain.model.AlertSeverity;

public record AlertCandidate(AlertSeverity severity, String message) {}

