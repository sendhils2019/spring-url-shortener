package com.example.urlshortener.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowStage {
    private final String id;
    private final String name;
    private final String summary;
    private final List<String> dependencies;
    private final boolean requiresApproval;
    private final int maxRetries;
    private final String branch;
    private final String syncGroup;
    private final String pathType;
    private final String entryGate;
    private final String exitGate;
    private final String fallbackStageId;
    private final Map<String, String> context;
    private final List<String> decisionTrace;
    private String status;
    private String decision;
    private int retryCount;
    private String rollbackTarget;

    // Execution progress simulation to support non-linear / parallel execution
    private int progress;
    private int requiredProgress;

    public WorkflowStage(String id, String name, String summary, List<String> dependencies, boolean requiresApproval) {
        this(id, name, summary, dependencies, requiresApproval, 2, "main", "linear");
    }

    public WorkflowStage(String id, String name, String summary, List<String> dependencies,
                         boolean requiresApproval, int maxRetries, String branch, String syncGroup) {
        this(id, name, summary, dependencies, requiresApproval, maxRetries, branch, syncGroup,
                "sequential", "entry:" + id, "exit:" + id, null);
    }

    public WorkflowStage(String id, String name, String summary, List<String> dependencies,
                         boolean requiresApproval, int maxRetries, String branch, String syncGroup,
                         String pathType, String entryGate, String exitGate, String fallbackStageId) {
        this.id = id;
        this.name = name;
        this.summary = summary;
        this.dependencies = dependencies == null ? new ArrayList<>() : new ArrayList<>(dependencies);
        this.requiresApproval = requiresApproval;
        this.maxRetries = Math.max(0, maxRetries);
        this.branch = branch;
        this.syncGroup = syncGroup;
        this.pathType = pathType == null ? "sequential" : pathType;
        this.entryGate = entryGate == null ? "entry:" + id : entryGate;
        this.exitGate = exitGate == null ? "exit:" + id : exitGate;
        this.fallbackStageId = fallbackStageId;
        this.context = new HashMap<>();
        this.decisionTrace = new ArrayList<>();
        this.status = "pending";
        this.decision = "awaiting";
        this.retryCount = 0;
        this.rollbackTarget = null;
        this.progress = 0;
        if ("implementation".equals(id) || "testing".equals(id)) {
            this.requiredProgress = 2;
        } else {
            this.requiredProgress = 1;
        }
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

    public int getProgress() {
        return progress;
    }

    public int getRequiredProgress() {
        return requiredProgress;
    }

    public void incrementProgress() {
        this.progress++;
    }

    public void resetProgress() {
        this.progress = 0;
    }

    public void setRequiredProgress(int requiredProgress) {
        this.requiredProgress = Math.max(1, requiredProgress);
    }

    public String getBranch() {
        return branch;
    }

    public String getSyncGroup() {
        return syncGroup;
    }

    public String getPathType() {
        return pathType;
    }

    public String getEntryGate() {
        return entryGate;
    }

    public String getExitGate() {
        return exitGate;
    }

    public String getFallbackStageId() {
        return fallbackStageId;
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
        this.decisionTrace.add(decision);
    }

    public Map<String, String> getContext() {
        return context;
    }

    public void setContext(String key, String value) {
        if (key != null && !key.isBlank()) {
            this.context.put(key, value);
        }
    }

    public List<String> getDecisionTrace() {
        return decisionTrace;
    }

    public void appendDecisionTrace(String trace) {
        if (trace != null && !trace.isBlank()) {
            this.decisionTrace.add(trace);
        }
    }
}
