package com.roboverse.fleet.api;

import com.roboverse.fleet.api.dto.AcknowledgeAlertRequest;
import com.roboverse.fleet.api.dto.AlertResponse;
import com.roboverse.fleet.api.dto.PagedResponse;
import com.roboverse.fleet.application.service.AlertService;
import com.roboverse.fleet.domain.model.AlertSeverity;
import com.roboverse.fleet.domain.model.AlertStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {
    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    @GetMapping
    public PagedResponse<AlertResponse> search(
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) String robotId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
        return PagedResponse.from(service.search(status, severity, robotId, page, size), AlertResponse::from);
    }

    @PostMapping("/{id}/acknowledge")
    public AlertResponse acknowledge(
            @PathVariable UUID id,
            @Valid @RequestBody AcknowledgeAlertRequest request) {
        return AlertResponse.from(service.acknowledge(id, request.operator()));
    }
}

