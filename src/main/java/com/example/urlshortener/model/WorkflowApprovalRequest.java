package com.example.urlshortener.model;

public record WorkflowApprovalRequest(String stageId, boolean approved, String approver, String rationale) {
}
