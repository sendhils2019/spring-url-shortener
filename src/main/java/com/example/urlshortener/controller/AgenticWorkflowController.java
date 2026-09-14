package com.example.urlshortener.controller;

import com.example.urlshortener.model.WorkflowApprovalRequest;
import com.example.urlshortener.model.WorkflowExecution;
import com.example.urlshortener.model.WorkflowMetrics;
import com.example.urlshortener.model.WorkflowRequest;
import com.example.urlshortener.service.AgenticWorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgenticWorkflowController {
    private final AgenticWorkflowService agenticWorkflowService;

    public AgenticWorkflowController(AgenticWorkflowService agenticWorkflowService) {
        this.agenticWorkflowService = agenticWorkflowService;
    }

    @PostMapping("/workflows")
    public ResponseEntity<Map<String, Object>> createWorkflow(@Valid @RequestBody WorkflowRequest request) {
        WorkflowExecution workflow = agenticWorkflowService.createWorkflow(request);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("scope", workflow.getScope());
        payload.put("normalizedProblem", workflow.getNormalizedProblem());
        payload.put("stages", workflow.getStages());
        payload.put("status", workflow.getStatus());
        payload.put("decisionLog", workflow.getDecisionLog());
        payload.put("policyGuardrails", workflow.getPolicyGuardrails());
        payload.put("impactAnalysis", workflow.getImpactAnalysis());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/workflows/{workflowId}")
    public ResponseEntity<Map<String, Object>> getWorkflow(@PathVariable String workflowId) {
        WorkflowExecution workflow = agenticWorkflowService.getWorkflow(workflowId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("requirement", workflow.getRequirement());
        payload.put("scope", workflow.getScope());
        payload.put("status", workflow.getStatus());
        payload.put("approvalState", workflow.getApprovalState());
        payload.put("decisionLog", workflow.getDecisionLog());
        payload.put("policyGuardrails", workflow.getPolicyGuardrails());
        payload.put("impactAnalysis", workflow.getImpactAnalysis());
        payload.put("stages", workflow.getStages());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/workflows/{workflowId}/advance")
    public ResponseEntity<Map<String, Object>> advanceWorkflow(@PathVariable String workflowId) {
        WorkflowExecution workflow = agenticWorkflowService.advanceWorkflow(workflowId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("status", workflow.getStatus());
        payload.put("decisionLog", workflow.getDecisionLog());
        payload.put("stages", workflow.getStages());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/workflows/{workflowId}/approve")
    public ResponseEntity<Map<String, Object>> approveStage(@PathVariable String workflowId, @Valid @RequestBody WorkflowApprovalRequest approval) {
        WorkflowExecution workflow = agenticWorkflowService.approveStage(workflowId, approval);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("status", workflow.getStatus());
        payload.put("approvalState", workflow.getApprovalState());
        payload.put("decisionLog", workflow.getDecisionLog());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/workflows/{workflowId}/replan")
    public ResponseEntity<Map<String, Object>> replanWorkflow(@PathVariable String workflowId) {
        WorkflowExecution workflow = agenticWorkflowService.replanWorkflow(workflowId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("status", workflow.getStatus());
        payload.put("decisionLog", workflow.getDecisionLog());
        payload.put("replanNotes", workflow.getReplanNotes());
        payload.put("stages", workflow.getStages());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/workflows/{workflowId}/retry")
    public ResponseEntity<Map<String, Object>> retryStage(@PathVariable String workflowId, @RequestBody Map<String, String> body) {
        String stageId = body.getOrDefault("stageId", "implementation");
        WorkflowExecution workflow = agenticWorkflowService.retryStage(workflowId, stageId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("status", workflow.getStatus());
        payload.put("decisionLog", workflow.getDecisionLog());
        payload.put("stages", workflow.getStages());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/workflows/{workflowId}/rollback")
    public ResponseEntity<Map<String, Object>> rollbackWorkflow(@PathVariable String workflowId, @RequestBody Map<String, String> body) {
        String targetStageId = body.getOrDefault("targetStageId", "implementation");
        WorkflowExecution workflow = agenticWorkflowService.rollbackWorkflow(workflowId, targetStageId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("status", workflow.getStatus());
        payload.put("rollbackTarget", workflow.getRollbackTarget());
        payload.put("decisionLog", workflow.getDecisionLog());
        payload.put("stages", workflow.getStages());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/workflows/{workflowId}/fallback")
    public ResponseEntity<Map<String, Object>> fallbackWorkflow(@PathVariable String workflowId, @RequestBody Map<String, String> body) {
        String stageId = body.getOrDefault("stageId", "implementation");
        String reason = body.get("reason");
        WorkflowExecution workflow = agenticWorkflowService.fallbackWorkflow(workflowId, stageId, reason);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("status", workflow.getStatus());
        payload.put("rollbackTarget", workflow.getRollbackTarget());
        payload.put("safeStopReason", workflow.getSafeStopReason());
        payload.put("decisionLog", workflow.getDecisionLog());
        return ResponseEntity.ok(payload);
    }

    @PostMapping("/workflows/{workflowId}/safe-stop")
    public ResponseEntity<Map<String, Object>> safeStopWorkflow(@PathVariable String workflowId, @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        WorkflowExecution workflow = agenticWorkflowService.safeStopWorkflow(workflowId, reason);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("status", workflow.getStatus());
        payload.put("safeStopReason", workflow.getSafeStopReason());
        payload.put("decisionLog", workflow.getDecisionLog());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/workflows/{workflowId}/guardrails")
    public ResponseEntity<Map<String, Object>> getGuardrails(@PathVariable String workflowId) {
        WorkflowExecution workflow = agenticWorkflowService.getWorkflow(workflowId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("workflowId", workflow.getId());
        payload.put("guardrails", workflow.getPolicyGuardrails());
        payload.put("impactAnalysis", workflow.getImpactAnalysis());
        payload.put("auditTrail", workflow.getAuditTrail());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        WorkflowMetrics metrics = agenticWorkflowService.getMetrics();
        Map<String, Object> payload = new HashMap<>();
        payload.put("successRate", metrics.getSuccessRate());
        payload.put("retryCount", metrics.getRetryCount());
        payload.put("rollbackCount", metrics.getRollbackCount());
        payload.put("mttrMinutes", metrics.getMttrMinutes());
        payload.put("endToEndLatencyMs", metrics.getEndToEndLatencyMs());
        return ResponseEntity.ok(payload);
    }
}
