package com.roboverse.fleet.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fleet")
public record FleetProperties(
        Topics topics,
        Cache cache,
        Security security,
        Websocket websocket,
        Alerting alerting,
        Ingestion ingestion) {

    public record Topics(
            String telemetry,
            String telemetryInvalid,
            String alerts) {}

    public record Cache(Duration latestTelemetryTtl) {}

    public record Security(
            boolean apiKeyEnabled,
            String apiKey,
            List<String> allowedOrigins) {}

    public record Websocket(
            String consumerGroup,
            boolean relayEnabled,
            String relayHost,
            int relayPort,
            String relayLogin,
            String relayPasscode) {}

    public record Alerting(
            double lowBatteryWarningPercent,
            double lowBatteryCriticalPercent,
            double highTemperatureCelsius,
            double stalledSpeedMps,
            Duration staleAfter) {}

    public record Ingestion(Duration maxClockSkew) {}
}
