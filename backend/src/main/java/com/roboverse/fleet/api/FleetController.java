package com.roboverse.fleet.api;

import com.roboverse.fleet.application.service.FleetSummaryService;
import com.roboverse.fleet.domain.model.FleetSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fleet")
public class FleetController {
    private final FleetSummaryService service;

    public FleetController(FleetSummaryService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public FleetSummary summary() {
        return service.getSummary();
    }
}

