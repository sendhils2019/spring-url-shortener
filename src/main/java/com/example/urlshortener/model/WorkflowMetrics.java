package com.example.urlshortener.model;

public class WorkflowMetrics {
    private final double successRate;
    private final int retryCount;
    private final int rollbackCount;
    private final double mttrMinutes;
    private final long endToEndLatencyMs;

    public WorkflowMetrics(double successRate, int retryCount, int rollbackCount, double mttrMinutes, long endToEndLatencyMs) {
        this.successRate = successRate;
        this.retryCount = retryCount;
        this.rollbackCount = rollbackCount;
        this.mttrMinutes = mttrMinutes;
        this.endToEndLatencyMs = endToEndLatencyMs;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getRollbackCount() {
        return rollbackCount;
    }

    public double getMttrMinutes() {
        return mttrMinutes;
    }

    public long getEndToEndLatencyMs() {
        return endToEndLatencyMs;
    }
}
