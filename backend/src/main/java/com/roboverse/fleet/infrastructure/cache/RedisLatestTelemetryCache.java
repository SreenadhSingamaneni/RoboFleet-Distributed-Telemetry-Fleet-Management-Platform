package com.roboverse.fleet.infrastructure.cache;

import com.roboverse.fleet.application.port.out.LatestTelemetryCachePort;
import com.roboverse.fleet.config.FleetProperties;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Repository;

@Repository
public class RedisLatestTelemetryCache implements LatestTelemetryCachePort {
    private static final Logger log = LoggerFactory.getLogger(RedisLatestTelemetryCache.class);
    private static final String PREFIX = "fleet:telemetry:latest:";

    private final RedisTemplate<String, TelemetryPoint> template;
    private final long ttlSeconds;
    private final Counter failures;

    public RedisLatestTelemetryCache(
            RedisTemplate<String, TelemetryPoint> template,
            FleetProperties properties,
            MeterRegistry registry) {
        this.template = template;
        ttlSeconds = properties.cache().latestTelemetryTtl().toSeconds();
        failures = Counter.builder("fleet.cache.failures").register(registry);
    }

    @Override
    public void putAll(List<TelemetryPoint> telemetry) {
        if (telemetry.isEmpty()) return;
        try {
            RedisSerializer<String> keySerializer = template.getStringSerializer();
            @SuppressWarnings("unchecked")
            RedisSerializer<TelemetryPoint> valueSerializer =
                    (RedisSerializer<TelemetryPoint>) template.getValueSerializer();
            template.executePipelined((RedisCallback<Object>) connection -> {
                for (TelemetryPoint point : telemetry) {
                    connection.stringCommands().setEx(
                            keySerializer.serialize(PREFIX + point.robotId()),
                            ttlSeconds,
                            valueSerializer.serialize(point));
                }
                return null;
            });
        } catch (DataAccessException exception) {
            failures.increment();
            log.warn("Redis write failed; durable ingestion continues", exception);
        }
    }

    @Override
    public Optional<TelemetryPoint> get(String robotId) {
        try {
            return Optional.ofNullable(template.opsForValue().get(PREFIX + robotId));
        } catch (DataAccessException exception) {
            failures.increment();
            log.warn("Redis read failed for robot {}", robotId, exception);
            return Optional.empty();
        }
    }
}

