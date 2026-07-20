package com.roboverse.fleet.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.roboverse.fleet.application.port.out.LatestTelemetryCachePort;
import com.roboverse.fleet.application.port.out.TelemetryStorePort;
import com.roboverse.fleet.domain.model.Connectivity;
import com.roboverse.fleet.domain.model.MissionState;
import com.roboverse.fleet.domain.model.TelemetryPoint;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TelemetryIngestionServiceTest {
    @Mock private TelemetryStorePort store;
    @Mock private LatestTelemetryCachePort cache;
    @Mock private AlertEvaluationService alerts;
    private TelemetryIngestionService service;

    @BeforeEach
    void setUp() {
        service = new TelemetryIngestionService(store, cache, alerts,
                new SimpleMeterRegistry());
    }

    @Test
    void only_fans_out_events_accepted_by_idempotent_store() {
        TelemetryPoint point = new TelemetryPoint(UUID.randomUUID(), "RBT-0001", 1,
                Instant.now(), 1, 1, 1, 70, 0.6, 90, 41,
                MissionState.IN_PROGRESS, Connectivity.CONNECTED, List.of());
        List<TelemetryPoint> batch = List.of(point);
        when(store.saveBatch(batch)).thenReturn(batch);

        int accepted = service.ingest(batch);

        assertThat(accepted).isEqualTo(1);
        verify(cache).putAll(batch);
        verify(alerts).evaluateBatch(batch);
    }
}
