package com.example.urlshortener;

import com.example.urlshortener.model.CreateLinkRequest;
import com.example.urlshortener.service.ShortUrlService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ShortUrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShortUrlService shortUrlService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        shortUrlService.reset();
    }

    @Test
    void healthEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void createLinkEndpointCreatesShortCode() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com/docs", "docs-demo", null, "key-1");

        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("docs-demo"));
    }

    @Test
    void redirectEndpointIncrementsClicks() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest("https://example.com", "redirect-demo", null, "key-2");
        mockMvc.perform(post("/api/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/redirect-demo")
                        .header("User-Agent", "test-agent")
                        .header("Referer", "https://example.com/landing"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com"));

        mockMvc.perform(get("/api/links/redirect-demo/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clicks").value(1));
    }
}
