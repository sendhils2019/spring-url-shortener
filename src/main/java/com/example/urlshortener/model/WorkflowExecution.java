package com.example.urlshortener.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class WorkflowExecution {
    private final String id;
    private final String requirement;
    private final String scope;
    private final String normalizedProblem;
    private final List<WorkflowStage> stages;
    private final Instant createdAt;
    private Instant updatedAt;
    private String status;
    private String approvalState;
    private final List<String> decisionLog;

    public WorkflowExecution(String id, String requirement, String scope, String normalizedProblem, List<WorkflowStage> stages) {
        this.id = id;
        this.requirement = requirement;
        this.scope = scope;
        this.normalizedProblem = normalizedProblem;
        this.stages = stages;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.status = "in_progress";
        this.approvalState = "pending";
        this.decisionLog = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getRequirement() {
        return requirement;
    }

    public String getScope() {
        return scope;
    }

    public String getNormalizedProblem() {
        return normalizedProblem;
    }

    public List<WorkflowStage> getStages() {
        return stages;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovalState() {
        return approvalState;
    }

    public void setApprovalState(String approvalState) {
        this.approvalState = approvalState;
    }

    public List<String> getDecisionLog() {
        return decisionLog;
    }

    public void addDecision(String message) {
        this.decisionLog.add(message);
    }
}
