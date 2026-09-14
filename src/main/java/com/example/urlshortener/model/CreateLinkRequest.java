package com.example.urlshortener.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateLinkRequest(
        @NotBlank(message = "URL is required")
        @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
        String url,
        String alias,
        String expiresAt,
        String idempotencyKey
) {
}
