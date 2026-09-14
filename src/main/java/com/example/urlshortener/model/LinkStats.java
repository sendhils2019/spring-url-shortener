package com.example.urlshortener.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class LinkStats {
    private final String code;
    private final String targetUrl;
    private final int clicks;
    private final int uniqueVisitors;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final boolean active;
    private final Map<String, Long> referrers;
    private final List<VisitEvent> recentVisits;

    public LinkStats(String code, String targetUrl, int clicks, int uniqueVisitors, Instant createdAt,
                    Instant expiresAt, boolean active, Map<String, Long> referrers, List<VisitEvent> recentVisits) {
        this.code = code;
        this.targetUrl = targetUrl;
        this.clicks = clicks;
        this.uniqueVisitors = uniqueVisitors;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
        this.referrers = referrers;
        this.recentVisits = recentVisits;
    }

    public String getCode() {
        return code;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public int getClicks() {
        return clicks;
    }

    public int getUniqueVisitors() {
        return uniqueVisitors;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public Map<String, Long> getReferrers() {
        return referrers;
    }

    public List<VisitEvent> getRecentVisits() {
        return recentVisits;
    }
}
