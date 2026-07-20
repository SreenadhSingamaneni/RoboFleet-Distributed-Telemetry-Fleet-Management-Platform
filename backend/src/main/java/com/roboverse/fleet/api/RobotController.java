package com.roboverse.fleet.api;

import com.roboverse.fleet.api.dto.PagedResponse;
import com.roboverse.fleet.api.dto.RobotResponse;
import com.roboverse.fleet.api.dto.TelemetryResponse;
import com.roboverse.fleet.application.service.RobotQueryService;
import com.roboverse.fleet.domain.model.RobotOperationalStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/robots")
public class RobotController {
    private final RobotQueryService service;

    public RobotController(RobotQueryService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<RobotResponse> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) RobotOperationalStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return PagedResponse.from(service.search(query, status, page, size), RobotResponse::from);
    }

    @GetMapping("/{id}")
    public RobotResponse get(@PathVariable String id) {
        return RobotResponse.from(service.get(id));
    }

    @GetMapping("/{id}/telemetry/latest")
    public ResponseEntity<TelemetryResponse> latest(@PathVariable String id) {
        return service.latestTelemetry(id)
                .map(TelemetryResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}/telemetry")
    public List<TelemetryResponse> history(
            @PathVariable String id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "500") @Min(1) @Max(10000) int limit) {
        Instant effectiveTo = to == null ? Instant.now() : to;
        Instant effectiveFrom = from == null ? effectiveTo.minus(1, ChronoUnit.HOURS) : from;
        return service.history(id, effectiveFrom, effectiveTo, limit).stream()
                .map(TelemetryResponse::from).toList();
    }
}

