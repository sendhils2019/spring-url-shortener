package com.example.urlshortener.model;

import java.time.Instant;

public class VisitEvent {
    private final Instant visitedAt;
    private final String userAgent;
    private final String referrer;
    private final String ipAddress;

    public VisitEvent(Instant visitedAt, String userAgent, String referrer, String ipAddress) {
        this.visitedAt = visitedAt;
        this.userAgent = userAgent;
        this.referrer = referrer;
        this.ipAddress = ipAddress;
    }

    public Instant getVisitedAt() {
        return visitedAt;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
