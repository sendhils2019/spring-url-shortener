package com.example.urlshortener.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /api/shorten and POST /api/links.
 * Merged: adds customAlias (from LinkController) alongside the original alias field.
 */
public record CreateLinkRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048)
        String url,

        /** Optional custom alias (e.g. "my-link"). */
        String customAlias,

        /** Kept for backward compatibility with existing callers. Maps to customAlias if set. */
        String alias,

        String expiresAt,
        String idempotencyKey
) {
    /** Returns the effective alias, preferring customAlias over the legacy alias field. */
    public String effectiveAlias() {
        if (customAlias != null && !customAlias.isBlank()) return customAlias;
        if (alias != null && !alias.isBlank()) return alias;
        return null;
    }
}
