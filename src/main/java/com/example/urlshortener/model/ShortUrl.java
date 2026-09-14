package com.example.urlshortener.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ShortUrl {
    private final String id;
    private final String code;
    private final String targetUrl;
    private final String shortUrl;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant expiresAt;
    private final boolean active;
    private final List<VisitEvent> analytics;
    private int clicks;

    public ShortUrl(String id, String code, String targetUrl, String shortUrl, Instant createdAt,
                    Instant updatedAt, Instant expiresAt, boolean active, List<VisitEvent> analytics, int clicks) {
        this.id = id;
        this.code = code;
        this.targetUrl = targetUrl;
        this.shortUrl = shortUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.analytics = analytics == null ? new ArrayList<>() : new ArrayList<>(analytics);
        this.clicks = clicks;
    }

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public List<VisitEvent> getAnalytics() {
        return analytics;
    }

    public int getClicks() {
        return clicks;
    }

    public void incrementClicks() {
        this.clicks++;
    }

    public void recordVisit(VisitEvent event) {
        analytics.add(event);
    }
}
