package com.example.urlshortener.model;

import java.util.List;

public class WorkflowStage {
    private final String id;
    private final String name;
    private final String summary;
    private final List<String> dependencies;
    private final boolean requiresApproval;
    private final int maxRetries;
    private final String branch;
    private final String syncGroup;
    private String status;
    private String decision;
    private int retryCount;
    private String rollbackTarget;

    public WorkflowStage(String id, String name, String summary, List<String> dependencies, boolean requiresApproval) {
        this(id, name, summary, dependencies, requiresApproval, 2, "main", "linear");
    }

    public WorkflowStage(String id, String name, String summary, List<String> dependencies,
                         boolean requiresApproval, int maxRetries, String branch, String syncGroup) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.dependencies = dependencies;
        this.requiresApproval = requiresApproval;
        this.maxRetries = Math.max(0, maxRetries);
        this.branch = branch;
        this.syncGroup = syncGroup;
        this.status = "pending";
        this.decision = "awaiting";
        this.retryCount = 0;
        this.rollbackTarget = null;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }

    public String getBranch() {
        return branch;
    }

    public String getSyncGroup() {
        return syncGroup;
    }

    public String getRollbackTarget() {
        return rollbackTarget;
    }

    public void setRollbackTarget(String rollbackTarget) {
        this.rollbackTarget = rollbackTarget;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }
}
