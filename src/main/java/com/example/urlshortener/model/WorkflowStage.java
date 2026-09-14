package com.example.urlshortener.model;

import java.util.List;

public class WorkflowStage {
    private final String id;
    private final String name;
    private final String summary;
    private final List<String> dependencies;
    private final boolean requiresApproval;
    private String status;
    private String decision;

    public WorkflowStage(String id, String name, String summary, List<String> dependencies, boolean requiresApproval) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.dependencies = dependencies;
        this.requiresApproval = requiresApproval;
        this.status = "pending";
        this.decision = "awaiting";
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
