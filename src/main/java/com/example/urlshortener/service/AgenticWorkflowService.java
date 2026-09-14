package com.example.urlshortener.service;

import com.example.urlshortener.exception.ApiException;
import com.example.urlshortener.model.WorkflowApprovalRequest;
import com.example.urlshortener.model.WorkflowExecution;
import com.example.urlshortener.model.WorkflowMetrics;
import com.example.urlshortener.model.WorkflowRequest;
import com.example.urlshortener.model.WorkflowStage;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AgenticWorkflowService {
    private final Map<String, WorkflowExecution> workflows = new ConcurrentHashMap<>();

    public WorkflowExecution createWorkflow(WorkflowRequest request) {
        if (request == null || request.requirement() == null || request.requirement().isBlank()) {
            throw new ApiException("A requirement is required to create an engineering workflow.");
        }

        String scope = request.scope() == null || request.scope().isBlank() ? "greenfield" : request.scope().toLowerCase();
        String normalizedProblem = normalizeRequirement(request.requirement(), scope);
        List<WorkflowStage> stages = buildStages(scope, request.requirement());
        WorkflowExecution workflow = new WorkflowExecution(
                UUID.randomUUID().toString(),
                request.requirement(),
                scope,
                normalizedProblem,
                stages
        );

        workflow.setPolicyGuardrails(evaluatePolicyGuardrails(request.requirement(), scope));
        workflow.setImpactAnalysis(evaluateImpactAnalysis(scope, request.requirement()));
        workflow.addDecision("Requirement normalized and scope classified as " + scope + ".");
        workflow.addDecision("Policy guardrails applied: " + String.join("; ", workflow.getPolicyGuardrails()));
        workflow.addDecision("Brownfield impact analysis: " + String.join("; ", workflow.getImpactAnalysis()));
        workflow.addDecision("Task graph created with explicit dependency gates, parallel execution branches, synchronization stages, and approval checkpoints.");
        workflows.put(workflow.getId(), workflow);
        return workflow;
    }

    public WorkflowExecution getWorkflow(String workflowId) {
        WorkflowExecution workflow = workflows.get(workflowId);
        if (workflow == null) {
            throw new ApiException("Workflow not found.");
        }
        return workflow;
    }

    public WorkflowExecution advanceWorkflow(String workflowId) {
        WorkflowExecution workflow = getWorkflow(workflowId);
        Map<String, WorkflowStage> byId = new LinkedHashMap<>();
        for (WorkflowStage stage : workflow.getStages()) {
            byId.put(stage.getId(), stage);
        }

        boolean progressed = false;
        for (WorkflowStage stage : workflow.getStages()) {
            if ("completed".equals(stage.getStatus()) || "blocked".equals(stage.getStatus()) || "failed".equals(stage.getStatus())) {
                if ("failed".equals(stage.getStatus()) && stage.getRetryCount() < stage.getMaxRetries()) {
                    stage.setStatus("pending");
                    stage.setDecision("retrying");
                    stage.incrementRetryCount();
                    workflow.addDecision(stage.getId(), "Retry " + stage.getRetryCount() + " of " + stage.getMaxRetries() + " has been scheduled for stage '" + stage.getName() + "'.");
                    progressed = true;
                }
                continue;
            }

            if (allDependenciesCompleted(stage, byId)) {
                if (stage.isRequiresApproval()) {
                    if (!"approved".equals(stage.getDecision())) {
                        workflow.addDecision(stage.getId(), "Stage '" + stage.getName() + "' is waiting for human approval.");
                        workflow.setStatus("waiting_for_approval");
                        continue;
                    }
                }

                if (hasPolicyViolation(stage, workflow)) {
                    stage.setStatus("blocked");
                    stage.setDecision("safe-stop");
                    workflow.setStatus("safe_stopped");
                    workflow.setApprovalState("rejected");
                    workflow.setSafeStopReason("Policy guardrails require intervention before this stage may continue.");
                    workflow.addDecision(stage.getId(), "Stage '" + stage.getName() + "' was blocked by policy guardrails.");
                    continue;
                }

                stage.setStatus("completed");
                stage.setDecision("executed");
                workflow.addDecision(stage.getId(), "Stage '" + stage.getName() + "' completed successfully.");
                progressed = true;
            }
        }

        if (!progressed && workflow.getStages().stream().allMatch(stage -> "completed".equals(stage.getStatus()))) {
            workflow.setStatus("completed");
            workflow.setApprovalState("closed");
            workflow.addDecision("All workflow stages finished. Release readiness approved and handoff closed.");
        }

        if (workflow.getStages().stream().anyMatch(stage -> "blocked".equals(stage.getStatus()) || "safe-stop".equals(stage.getDecision()))) {
            workflow.setStatus("safe_stopped");
        }

        workflow.setUpdatedAt(Instant.now());
        return workflow;
    }

    public WorkflowExecution approveStage(String workflowId, WorkflowApprovalRequest approval) {
        WorkflowExecution workflow = getWorkflow(workflowId);
        WorkflowStage stage = workflow.getStages().stream()
                .filter(item -> item.getId().equals(approval.stageId()))
                .findFirst()
                .orElseThrow(() -> new ApiException("Stage not found in workflow."));

        if (approval.approved()) {
            stage.setDecision("approved");
            stage.setStatus("approved");
            workflow.setApprovalState("approved");
            workflow.addDecision(stage.getId(), "Human approval granted for stage '" + stage.getName() + "' by " + approval.approver() + ".");
            workflow.setStatus("in_progress");
        } else {
            stage.setDecision("rejected");
            stage.setStatus("blocked");
            workflow.setStatus("safe_stopped");
            workflow.setApprovalState("rejected");
            workflow.setSafeStopReason(approval.rationale() == null ? "not provided" : approval.rationale());
            workflow.addDecision(stage.getId(), "Human approval rejected for stage '" + stage.getName() + "'. Rationale: " + workflow.getSafeStopReason());
        }

        workflow.setUpdatedAt(Instant.now());
        return workflow;
    }

    public WorkflowExecution retryStage(String workflowId, String stageId) {
        WorkflowExecution workflow = getWorkflow(workflowId);
        WorkflowStage stage = workflow.getStages().stream()
                .filter(item -> item.getId().equals(stageId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Stage not found in workflow."));

        if (stage.getRetryCount() >= stage.getMaxRetries()) {
            throw new ApiException("Stage has reached its retry limit. Safe-stop protection has been activated.");
        }

        stage.incrementRetryCount();
        stage.setStatus("pending");
        stage.setDecision("retrying");
        workflow.setStatus("in_progress");
        workflow.addDecision(stage.getId(), "Retry scheduled for stage '" + stage.getName() + "'. Attempt " + stage.getRetryCount() + " of " + stage.getMaxRetries() + ".");
        workflow.setUpdatedAt(Instant.now());
        return workflow;
    }

    public WorkflowExecution rollbackWorkflow(String workflowId, String targetStageId) {
        WorkflowExecution workflow = getWorkflow(workflowId);
        WorkflowStage target = workflow.getStages().stream()
                .filter(item -> item.getId().equals(targetStageId))
                .findFirst()
                .orElseThrow(() -> new ApiException("Rollback target stage not found in workflow."));

        for (WorkflowStage stage : workflow.getStages()) {
            if (stage.getId().equals(targetStageId)) {
                stage.setStatus("pending");
                stage.setDecision("rolled_back");
                workflow.setRollbackTarget(targetStageId);
                workflow.addDecision(stage.getId(), "Rollback initiated to stage '" + stage.getName() + "'.");
                continue;
            }

            if (workflow.getStages().indexOf(stage) > workflow.getStages().indexOf(target)) {
                stage.setStatus("pending");
                stage.setDecision("awaiting");
            }
        }

        workflow.setStatus("rolled_back");
        workflow.setApprovalState("pending");
        workflow.setUpdatedAt(Instant.now());
        return workflow;
    }

    public WorkflowExecution replanWorkflow(String workflowId) {
        WorkflowExecution workflow = getWorkflow(workflowId);
        List<String> findings = new ArrayList<>();

        if (workflow.getScope().equals("brownfield") || workflow.getRequirement().toLowerCase().contains("brownfield")) {
            findings.add("Complete change-impact analysis of URL validation, redirect flow, API compatibility, workflow graph, and analytics before implementation.");
        }
        if (workflow.getRequirement().toLowerCase().contains("ambiguous") || workflow.getRequirement().toLowerCase().contains("unclear")) {
            findings.add("Clarify missing acceptance criteria before implementation proceeds.");
        }
        if (workflow.getRequirement().toLowerCase().contains("security") || workflow.getRequirement().toLowerCase().contains("compliance") || workflow.getRequirement().toLowerCase().contains("policy")) {
            findings.add("Enforce secret scanning, dependency validation, and approval gating before release.");
        }

        if (!findings.isEmpty()) {
            workflow.setReplanNotes(findings);
            workflow.addDecision("Dynamic re-planning triggered. Revised execution plan: " + String.join("; ", findings));
            workflow.setStatus("in_progress");
        }

        workflow.setUpdatedAt(Instant.now());
        return workflow;
    }

    public WorkflowMetrics getMetrics() {
        int workflowCount = workflows.size();
        int completedCount = (int) workflows.values().stream().filter(item -> "completed".equals(item.getStatus())).count();
        double successRate = workflowCount == 0 ? 0.0 : (completedCount * 100.0) / workflowCount;
        int retryCount = 0;
        int rollbackCount = 0;
        double mttrMinutes = 0.0;
        long endToEndLatencyMs = 0L;

        for (WorkflowExecution workflow : workflows.values()) {
            retryCount += workflow.getStages().stream().mapToInt(WorkflowStage::getRetryCount).sum();
            if ("rolled_back".equals(workflow.getStatus())) {
                rollbackCount++;
            }
            if (workflow.getCreatedAt() != null && workflow.getUpdatedAt() != null) {
                long deltaMs = Duration.between(workflow.getCreatedAt(), workflow.getUpdatedAt()).toMillis();
                endToEndLatencyMs += Math.max(deltaMs, 0L);
            }
        }

        if (!workflows.isEmpty()) {
            mttrMinutes = Math.max(1.0, endToEndLatencyMs / (workflows.size() * 60000.0));
        }

        return new WorkflowMetrics(successRate, retryCount, rollbackCount, mttrMinutes, endToEndLatencyMs);
    }

    private boolean allDependenciesCompleted(WorkflowStage stage, Map<String, WorkflowStage> byId) {
        for (String dependencyId : stage.getDependencies()) {
            WorkflowStage dependency = byId.get(dependencyId);
            if (dependency == null || !"completed".equals(dependency.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasPolicyViolation(WorkflowStage stage, WorkflowExecution workflow) {
        if (workflow.getPolicyGuardrails() == null || workflow.getPolicyGuardrails().isEmpty()) {
            return false;
        }

        String stageId = stage.getId();
        return "implementation".equals(stageId) && workflow.getPolicyGuardrails().stream().anyMatch(item -> item.toLowerCase().contains("secret") || item.toLowerCase().contains("compliance"));
    }

    private List<WorkflowStage> buildStages(String scope, String requirement) {
        List<WorkflowStage> stages = new ArrayList<>();
        stages.add(new WorkflowStage("requirement-analysis", "Requirement Analysis", "Interpret intent, identify ambiguity, and normalize into a clear engineering problem.", List.of(), false, 1, "discovery", "analysis"));
        stages.add(new WorkflowStage("task-decomposition", "Task Decomposition", "Break the requirement into actionable tasks, sequencing, and dependency ordering.", List.of("requirement-analysis"), false, 1, "planning", "analysis"));
        stages.add(new WorkflowStage("implementation", "Implementation", "Implement the URL shortener service and supporting API contracts.", List.of("task-decomposition"), false, 2, "delivery", "parallel"));
        stages.add(new WorkflowStage("documentation", "Documentation", "Document design assumptions, trade-offs, and usage guidance.", List.of("task-decomposition"), false, 1, "documentation", "parallel"));
        stages.add(new WorkflowStage("testing", "Testing", "Run critical validation, edge-case checks, integration verification, and synchronization across parallel branches.", List.of("implementation", "documentation"), false, 2, "verification", "synchronization"));

        boolean requiresRiskReview = scope.equals("brownfield") || requirement.toLowerCase().contains("brownfield") || requirement.toLowerCase().contains("security") || requirement.toLowerCase().contains("compliance") || requirement.toLowerCase().contains("policy") || requirement.toLowerCase().contains("ambiguous") || requirement.toLowerCase().contains("unclear");
        if (requiresRiskReview) {
            stages.add(new WorkflowStage("risk-review", "Risk Review", "Perform brownfield impact analysis, policy compliance review, and change control verification before release.", List.of("testing"), false, 2, "governance", "synchronization"));
        }

        List<String> releaseDependencies = new ArrayList<>(List.of("testing"));
        if (requiresRiskReview) {
            releaseDependencies.add("risk-review");
        }
        stages.add(new WorkflowStage("release-readiness", "Release Readiness", "Human approval gate to confirm governance, risk controls, and final readiness.", releaseDependencies, true, 2, "release", "approval"));
        return stages;
    }

    private List<String> evaluatePolicyGuardrails(String requirement, String scope) {
        List<String> guardrails = new ArrayList<>();
        String lower = requirement.toLowerCase();

        if (lower.contains("security") || lower.contains("compliance") || lower.contains("policy") || lower.contains("sensitive") || lower.contains("secret")) {
            guardrails.add("Secret scanning and access-control review are required before release.");
        }

        if (scope.equals("brownfield") || lower.contains("brownfield")) {
            guardrails.add("Brownfield impact analysis and compatibility checks are required before implementation approval.");
        }

        if (lower.contains("ambiguous") || lower.contains("unclear")) {
            guardrails.add("Requirements must be clarified and normalized before implementation proceeds.");
        }

        if (lower.contains("api") || lower.contains("database") || lower.contains("integration")) {
            guardrails.add("Dependency and contract validation is mandatory for all integration points.");
        }

        if (guardrails.isEmpty()) {
            guardrails.add("Standard validation and human approval gate apply.");
        }

        return guardrails;
    }

    private List<String> evaluateImpactAnalysis(String scope, String requirement) {
        List<String> impact = new ArrayList<>();
        String lower = requirement.toLowerCase();

        impact.add("ShortUrlService: URL validation, alias normalization, and redirect logic");
        impact.add("ShortUrlController: HTTP contract and redirect behavior");
        impact.add("AgenticWorkflowService: dependency graph and approval gate orchestration");
        impact.add("In-memory link state and analytics pipeline: click tracking and reporting");

        if (scope.equals("brownfield") || lower.contains("brownfield")) {
            impact.add("Compatibility review for existing API consumers and decision logging");
            impact.add("Regression validation for redirect and analytics behavior");
        }

        if (lower.contains("security") || lower.contains("compliance") || lower.contains("policy")) {
            impact.add("Security review for input validation, secrets handling, and deployment control");
        }

        return impact;
    }

    private String normalizeRequirement(String requirement, String scope) {
        String normalized = requirement.trim();
        String lower = normalized.toLowerCase();

        StringBuilder problem = new StringBuilder();
        problem.append("Build a URL shortener service with controlled autonomy for a ")
                .append(scope)
                .append(" scenario. The system will interpret the requirement, decompose tasks, execute implementation in parallel branches where possible, synchronize verification, and gate final release readiness.");

        if (lower.contains("brownfield")) {
            problem.append(" This includes identifying impacted APIs, modules, and data flows before changes.");
        }

        if (lower.contains("ambiguous") || lower.contains("unclear")) {
            problem.append(" Ambiguities are explicitly tracked, clarified, and normalized before implementation.");
        }

        if (lower.contains("security") || lower.contains("compliance") || lower.contains("policy")) {
            problem.append(" Security and policy guardrails are enforced across the lifecycle.");
        }

        return problem.toString();
    }
}
