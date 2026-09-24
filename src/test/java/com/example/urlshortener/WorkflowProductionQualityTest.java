package com.example.urlshortener;

import com.example.urlshortener.model.WorkflowRequest;
import com.example.urlshortener.model.WorkflowStage;
import com.example.urlshortener.service.AgenticWorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "shortener.db-path=:memory:",
        "shortener.base-url=http://localhost:8080",
        "shortener.create-rate-per-minute=1000"
})
class WorkflowProductionQualityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgenticWorkflowService agenticWorkflowService;

    @Test
    void invalidWorkflowRequestIsRejected() throws Exception {
        mockMvc.perform(post("/api/agent/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requirement\":\"\",\"scope\":\"greenfield\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replanAddsGovernanceGateWhenRequirementsChange() {
        var workflow = agenticWorkflowService.createWorkflow(
                new WorkflowRequest("Ambiguous and security-sensitive revision for the shortener service.", "greenfield")
        );

        agenticWorkflowService.replanWorkflow(workflow.getId());

        assertThat(workflow.getReplanNotes()).isNotEmpty();
        assertThat(workflow.getStages())
                .extracting(WorkflowStage::getId)
                .contains("risk-review");
    }

    @Test
    void fallbackAndSafeStopAreAuditedAndTrackGovernance() {
        var workflow = agenticWorkflowService.createWorkflow(
                new WorkflowRequest("Brownfield update for secure URL shortener with analytics policy review.", "brownfield")
        );

        agenticWorkflowService.fallbackWorkflow(workflow.getId(), "implementation", "Dependency drift detected.");
        agenticWorkflowService.safeStopWorkflow(workflow.getId(), "Manual intervention required.");

        assertThat(workflow.getRollbackTarget()).isNotBlank();
        assertThat(workflow.getStatus()).isEqualTo("safe_stopped");
        assertThat(workflow.getDecisionLog()).anyMatch(message -> message.contains("Safe-stop"));
        assertThat(workflow.getAuditTrail()).isNotEmpty();
    }

    @Test
    void metricsEndpointExposesReliabilitySignals() throws Exception {
        var workflow = agenticWorkflowService.createWorkflow(
                new WorkflowRequest("Brownfield update for secure URL shortener with analytics policy review.", "brownfield")
        );
        agenticWorkflowService.safeStopWorkflow(workflow.getId(), "Manual governance stop.");

        mockMvc.perform(get("/api/agent/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successRate").exists())
                .andExpect(jsonPath("$.retryCount").exists())
                .andExpect(jsonPath("$.rollbackCount").exists())
                .andExpect(jsonPath("$.mttrMinutes").exists())
                .andExpect(jsonPath("$.endToEndLatencyMs").exists());
    }
}
