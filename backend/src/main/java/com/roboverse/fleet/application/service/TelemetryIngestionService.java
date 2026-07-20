package com.roboverse.fleet.application.service;

import com.roboverse.fleet.application.port.out.LatestTelemetryCachePort;
import com.roboverse.fleet.application.port.out.TelemetryStorePort;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TelemetryIngestionService {
    private final TelemetryStorePort store;
    private final LatestTelemetryCachePort cache;
    private final AlertEvaluationService alertEvaluation;
    private final Counter acceptedCounter;
    private final Counter duplicateCounter;
    private final Timer ingestionTimer;

    public TelemetryIngestionService(
            TelemetryStorePort store,
            LatestTelemetryCachePort cache,
            AlertEvaluationService alertEvaluation,
            MeterRegistry registry) {
        this.store = store;
        this.cache = cache;
        this.alertEvaluation = alertEvaluation;
        acceptedCounter = Counter.builder("fleet.telemetry.accepted").register(registry);
        duplicateCounter = Counter.builder("fleet.telemetry.duplicates").register(registry);
        ingestionTimer = Timer.builder("fleet.telemetry.ingestion.duration").publishPercentileHistogram().register(registry);
    }

    public int ingest(List<TelemetryPoint> batch) {
        return ingestionTimer.record(() -> {
            List<TelemetryPoint> accepted = store.saveBatch(batch);
            acceptedCounter.increment(accepted.size());
            duplicateCounter.increment(batch.size() - accepted.size());
            if (!accepted.isEmpty()) {
                cache.putAll(accepted);
                alertEvaluation.evaluateBatch(accepted);
            }
            return accepted.size();
        });
    }
}
