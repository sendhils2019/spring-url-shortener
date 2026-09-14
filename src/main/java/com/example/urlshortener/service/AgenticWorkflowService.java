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
        WorkflowExecution workflow = new WorkflowExecution(
                UUID.randomUUID().toString(),
                request.requirement(),
                scope,
                normalizedProblem,
                buildStages()
        );

        workflow.addDecision("Requirement normalized and scope classified as " + scope + ".");
        workflow.addDecision("Task graph created with explicit dependency gates and approval checkpoints.");
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
            if ("completed".equals(stage.getStatus())) {
                continue;
            }

            if (allDependenciesCompleted(stage, byId)) {
                if (stage.isRequiresApproval()) {
                    if (!"approved".equals(stage.getDecision())) {
                        workflow.addDecision("Stage '" + stage.getName() + "' is waiting for human approval.");
                        continue;
                    }
                }

                stage.setStatus("completed");
                stage.setDecision("executed");
                workflow.addDecision("Stage '" + stage.getName() + "' completed successfully.");
                progressed = true;
            }
        }

        if (!progressed && workflow.getStages().stream().allMatch(stage -> "completed".equals(stage.getStatus()))) {
            workflow.setStatus("completed");
            workflow.setApprovalState("closed");
            workflow.addDecision("All workflow stages finished. Release readiness approved and handoff closed.");
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
            workflow.addDecision("Human approval granted for stage '" + stage.getName() + "' by " + approval.approver() + ".");
        } else {
            stage.setDecision("rejected");
            stage.setStatus("blocked");
            workflow.setStatus("blocked");
            workflow.setApprovalState("rejected");
            workflow.addDecision("Human approval rejected for stage '" + stage.getName() + "'. Rationale: " + (approval.rationale() == null ? "not provided" : approval.rationale()));
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

    private List<WorkflowStage> buildStages() {
        List<WorkflowStage> stages = new ArrayList<>();
        stages.add(new WorkflowStage("requirement-analysis", "Requirement Analysis", "Interpret intent, identify ambiguity, and normalize into a clear engineering problem.", List.of(), false));
        stages.add(new WorkflowStage("task-decomposition", "Task Decomposition", "Break the requirement into actionable tasks, sequencing, and dependency ordering.", List.of("requirement-analysis"), false));
        stages.add(new WorkflowStage("implementation", "Implementation", "Implement the URL shortener service and supporting API contracts.", List.of("task-decomposition"), false));
        stages.add(new WorkflowStage("testing", "Testing", "Run critical validation, edge-case checks, and integration verification.", List.of("implementation"), false));
        stages.add(new WorkflowStage("documentation", "Documentation", "Document design assumptions, trade-offs, and usage guidance.", List.of("implementation"), false));
        stages.add(new WorkflowStage("release-readiness", "Release Readiness", "Human approval gate to confirm governance, risk controls, and final readiness.", List.of("testing", "documentation"), true));
        return stages;
    }

    private String normalizeRequirement(String requirement, String scope) {
        String normalized = requirement.trim();
        String lower = normalized.toLowerCase();

        StringBuilder problem = new StringBuilder();
        problem.append("Build a URL shortener service with controlled autonomy for a ")
                .append(scope)
                .append(" scenario. The system will interpret the requirement, decompose tasks, execute implementation, validate with tests, generate documentation, and gate final release readiness.");

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
