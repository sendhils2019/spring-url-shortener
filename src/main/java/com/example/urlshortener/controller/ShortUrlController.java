package com.example.urlshortener.controller;

import com.example.urlshortener.config.ShortenerProperties;
import com.example.urlshortener.exception.ApiException;
import com.example.urlshortener.model.CreateLinkRequest;
import com.example.urlshortener.repository.Link;
import com.example.urlshortener.service.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ShortUrlController {

    private final ShortUrlService shortUrlService;
    private final ShortenerProperties props;

    public ShortUrlController(ShortUrlService shortUrlService, ShortenerProperties props) {
        this.shortUrlService = shortUrlService;
        this.props = props;
    }

    // ── Health ──────────────────────────────────────────────────────────────

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    // ── Create (two accepted paths for compatibility) ────────────────────────

    /** Spec-compliant path: POST /api/shorten */
    @PostMapping("/api/shorten")
    public ResponseEntity<Map<String, Object>> shorten(@Valid @RequestBody CreateLinkRequest request) {
        return createLink(request);
    }

    /** Original path: POST /api/links */
    @PostMapping("/api/links")
    public ResponseEntity<Map<String, Object>> createLink(@Valid @RequestBody CreateLinkRequest request) {
        ShortUrlService.LinkResult result = shortUrlService.createShortUrl(request);
        Link link = result.link();
        Map<String, Object> body = linkMap(link);
        HttpStatus status = result.isNew() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(body);
    }

    // ── Lookup ──────────────────────────────────────────────────────────────

    @GetMapping("/api/links/{code}")
    public ResponseEntity<Map<String, Object>> getLink(@PathVariable String code) {
        Link link = shortUrlService.getByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "short link not found"));
        return ResponseEntity.ok(linkMap(link));
    }

    @GetMapping("/api/links/{code}/stats")
    public ResponseEntity<Map<String, Object>> getStats(@PathVariable String code) {
        return ResponseEntity.ok(shortUrlService.getStats(code));
    }

    // ── Delete ──────────────────────────────────────────────────────────────

    @DeleteMapping("/api/links/{code}")
    public ResponseEntity<Void> deleteLink(@PathVariable String code) {
        if (!shortUrlService.deleteLink(code)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "short link not found");
        }
        return ResponseEntity.noContent().build();
    }

    // ── Redirect ────────────────────────────────────────────────────────────

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String clientKey = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        String referrer  = request.getHeader("Referer");
        Link link = shortUrlService.recordVisit(code, clientKey, referrer);
        // 307 (not 301/302): browsers must not cache the redirect, or clicks go uncounted.
        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
                .header(HttpHeaders.LOCATION, link.url())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private Map<String, Object> linkMap(Link link) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", link.code());
        m.put("short_url", props.getBaseUrl() + "/" + link.code());
        m.put("url", link.url());
        m.put("created_at", link.createdAt());
        m.put("is_custom", link.isCustom());
        return m;
    }
}
