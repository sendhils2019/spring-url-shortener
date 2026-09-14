package com.example.urlshortener.model;

import jakarta.validation.constraints.NotBlank;

public record WorkflowRequest(
        @NotBlank(message = "Requirement is required")
        String requirement,
        String scope
) {
}
