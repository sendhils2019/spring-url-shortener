package com.example.urlshortener;

import com.example.urlshortener.model.CreateLinkRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ShortUrlController.
 *
 * Uses an in-memory SQLite DB (shortener.db-path=:memory:) so every test
 * class gets a fresh database — no shared state, no reset() needed.
 *
 * Note: individual @Test methods within this class share the same Spring
 * context (and the same :memory: DB). If you need test-level isolation,
 * split tests into separate classes each annotated with @TestPropertySource.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "shortener.db-path=:memory:",
        "shortener.base-url=http://localhost:8080",
        "shortener.create-rate-per-minute=1000"   // high limit so tests never hit 429
})
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Health ───────────────────────────────────────────────────────────────

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Test
    void createLink_withCustomAlias_returns201AndCorrectCode() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest(
                "https://example.com/docs",   // url
                "docs-alias",                  // customAlias
                null,                          // alias (legacy)
                null,                          // expiresAt
                null                           // idempotencyKey
        );

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("docs-alias"))
                .andExpect(jsonPath("$.short_url").value("http://localhost:8080/docs-alias"))
                .andExpect(jsonPath("$.url").value("https://example.com/docs"))
                .andExpect(jsonPath("$.is_custom").value(true));
    }

    @Test
    void createLink_sameUrlTwice_secondReturns200_dedup() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest(
                "https://dedup-test.example.com", null, null, null, null);
        String body = objectMapper.writeValueAsString(request);

        // First call → 201
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Second call with same URL → 200 (dedup)
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://dedup-test.example.com"));
    }

    @Test
    void createLink_duplicateAlias_returns409() throws Exception {
        CreateLinkRequest first = new CreateLinkRequest(
                "https://a.example.com", "my-alias", null, null, null);
        CreateLinkRequest second = new CreateLinkRequest(
                "https://b.example.com", "my-alias", null, null, null);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void createLink_invalidUrl_returns422() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest(
                "ftp://not-allowed.com", null, null, null, null);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void createLink_reservedAlias_returns422() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest(
                "https://example.com", "api", null, null, null);

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("custom_alias 'api' is reserved"));
    }

    // ── Redirect ─────────────────────────────────────────────────────────────

    @Test
    void redirect_returns307_andClickIsRecorded() throws Exception {
        // Create the link first
        CreateLinkRequest request = new CreateLinkRequest(
                "https://redirect-target.example.com", "redir-test", null, null, null);
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Follow the redirect — expect 307
        mockMvc.perform(get("/redir-test")
                        .header("Referer", "https://referrer.example.com"))
                .andExpect(status().isTemporaryRedirect())
                .andExpect(header().string("Location", "https://redirect-target.example.com"))
                .andExpect(header().string("Cache-Control", "no-store"));

        // Give the async ClickRecorder a moment to write
        Thread.sleep(100);

        // Stats should show 1 click
        mockMvc.perform(get("/api/links/redir-test/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_clicks").value(1));
    }

    @Test
    void redirect_unknownCode_returns404() throws Exception {
        mockMvc.perform(get("/no-such-code-xyz"))
                .andExpect(status().isNotFound());
    }

    // ── Lookup & Delete ──────────────────────────────────────────────────────

    @Test
    void getLink_returnsCorrectFields() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest(
                "https://lookup.example.com", "lookup-alias", null, null, null);
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/links/lookup-alias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("lookup-alias"))
                .andExpect(jsonPath("$.url").value("https://lookup.example.com"))
                .andExpect(jsonPath("$.short_url").value("http://localhost:8080/lookup-alias"));
    }

    @Test
    void deleteLink_returns204_thenGetReturns404() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest(
                "https://delete-me.example.com", "delete-alias", null, null, null);
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/links/delete-alias"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/links/delete-alias"))
                .andExpect(status().isNotFound());
    }
}
