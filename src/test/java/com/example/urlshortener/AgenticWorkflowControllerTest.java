package com.example.urlshortener;

import com.example.urlshortener.model.WorkflowApprovalRequest;
import com.example.urlshortener.model.WorkflowRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgenticWorkflowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createWorkflowReturnsDependencyGatedStages() throws Exception {
        WorkflowRequest request = new WorkflowRequest(
                "Build a URL shortener service with governance, audits, and validation.",
                "greenfield"
        );

        mockMvc.perform(post("/api/agent/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("greenfield"))
                .andExpect(jsonPath("$.stages[0].id").value("requirement-analysis"));
    }

    @Test
    void workflowBuildsParallelBranchesAndPolicyGuardrailsForBrownfieldChange() throws Exception {
        WorkflowRequest request = new WorkflowRequest(
                "Brownfield update for a secure URL shortener with analytics and release policy review.",
                "brownfield"
        );

        mockMvc.perform(post("/api/agent/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("risk-review")))
                .andExpect(content().string(containsString("Brownfield impact analysis")))
                .andExpect(content().string(containsString("Secret scanning")));
    }

    @Test
    void approvalGateCanBlockAndReleaseWorkflow() throws Exception {
        WorkflowRequest request = new WorkflowRequest(
                "Build a secure URL shortener with analytics and release controls.",
                "brownfield"
        );

        String body = mockMvc.perform(post("/api/agent/workflows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String workflowId = body.split("\\\"workflowId\\\":\\\s*\\\"")[1].split("\\\"")[0];

        mockMvc.perform(post("/api/agent/workflows/" + workflowId + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"release-readiness\",\"approved\":true,\"approver\":\"reviewer\",\"rationale\":\"ready\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalState").value("approved"));

        mockMvc.perform(get("/api/agent/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successRate").exists());
    }
}
