package com.roboverse.fleet.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcknowledgeAlertRequest(
        @NotBlank @Size(max = 120) String operator) {}

