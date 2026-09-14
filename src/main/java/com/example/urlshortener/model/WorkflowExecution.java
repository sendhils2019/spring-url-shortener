package com.example.urlshortener.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final List<Map<String, String>> auditTrail;
    private final Map<String, String> crossStageContext;

    // New fields required by controllers and services
    private List<String> policyGuardrails;
    private List<String> impactAnalysis;
    private List<String> replanNotes;
    private String rollbackTarget;
    private String safeStopReason;

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
        this.auditTrail = new ArrayList<>();
        this.crossStageContext = new LinkedHashMap<>();
        this.policyGuardrails = new ArrayList<>();
        this.impactAnalysis = new ArrayList<>();
        this.replanNotes = new ArrayList<>();
        this.rollbackTarget = null;
        this.safeStopReason = null;
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

    public List<Map<String, String>> getAuditTrail() {
        return auditTrail;
    }

    public Map<String, String> getCrossStageContext() {
        return crossStageContext;
    }

    public void recordContext(String key, String value) {
        if (key != null && !key.isBlank()) {
            this.crossStageContext.put(key, value);
        }
    }

    public void addAuditEntry(String stageId, String message) {
        Map<String, String> record = new LinkedHashMap<>();
        record.put("timestamp", Instant.now().toString());
        record.put("stageId", stageId == null ? "workflow" : stageId);
        record.put("message", message == null ? "" : message);
        this.auditTrail.add(record);
    }

    // Existing single-arg decision logger
    public void addDecision(String message) {
        this.decisionLog.add(message);
        addAuditEntry("workflow", message);
    }

    // Overload used by services to prefix decisions with a stage id or context
    public void addDecision(String contextId, String message) {
        if (contextId == null || contextId.isBlank()) {
            addDecision(message);
            return;
        }
        String entry = "[" + contextId + "] " + message;
        this.decisionLog.add(entry);
        addAuditEntry(contextId, message);
    }

    // Policy guardrails
    public List<String> getPolicyGuardrails() {
        return policyGuardrails;
    }

    public void setPolicyGuardrails(List<String> policyGuardrails) {
        this.policyGuardrails = policyGuardrails == null ? new ArrayList<>() : new ArrayList<>(policyGuardrails);
    }

    // Impact analysis
    public List<String> getImpactAnalysis() {
        return impactAnalysis;
    }

    public void setImpactAnalysis(List<String> impactAnalysis) {
        this.impactAnalysis = impactAnalysis == null ? new ArrayList<>() : new ArrayList<>(impactAnalysis);
    }

    // Replan notes
    public List<String> getReplanNotes() {
        return replanNotes;
    }

    public void setReplanNotes(List<String> replanNotes) {
        this.replanNotes = replanNotes == null ? new ArrayList<>() : new ArrayList<>(replanNotes);
    }

    // Rollback target
    public String getRollbackTarget() {
        return rollbackTarget;
    }

    public void setRollbackTarget(String rollbackTarget) {
        this.rollbackTarget = rollbackTarget;
    }

    // Safe-stop reason
    public String getSafeStopReason() {
        return safeStopReason;
    }

    public void setSafeStopReason(String safeStopReason) {
        this.safeStopReason = safeStopReason;
    }
}
