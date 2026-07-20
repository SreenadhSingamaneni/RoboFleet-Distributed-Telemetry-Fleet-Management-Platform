package com.roboverse.fleet.infrastructure.messaging;

import java.time.Instant;

public record InvalidTelemetryEvent(
        TelemetryEvent event,
        String reason,
        Instant rejectedAt) {}

