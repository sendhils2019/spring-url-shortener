package com.example.urlshortener.model;

import jakarta.validation.constraints.NotBlank;

public record WorkflowApprovalRequest(
        @NotBlank(message = "Stage ID is required")
        String stageId,
        boolean approved,
        String approver,
        String rationale
) {
}
