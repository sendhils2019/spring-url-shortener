package com.example.urlshortener.controller;

import com.example.urlshortener.model.CreateLinkRequest;
import com.example.urlshortener.model.LinkStats;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.service.ShortUrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ShortUrlController {
    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "ok");
        payload.put("service", "url-shortener");
        return payload;
    }

    @GetMapping("/api/links")
    public Map<String, Object> listLinks() {
        List<ShortUrl> links = shortUrlService.listLinks();
        Map<String, Object> payload = new HashMap<>();
        payload.put("count", links.size());
        payload.put("items", links);
        return payload;
    }

    @PostMapping("/api/links")
    public ResponseEntity<Map<String, Object>> createLink(@Valid @RequestBody CreateLinkRequest request) {
        ShortUrl created = shortUrlService.createShortUrl(request);
        Map<String, Object> payload = new HashMap<>();
        payload.put("success", true);
        payload.put("data", created);
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }

    @GetMapping("/api/links/{code}")
    public ResponseEntity<?> getLink(@PathVariable String code) {
        ShortUrl link = shortUrlService.getByCode(code);
        if (link == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Shortened link not found."));
        }
        return ResponseEntity.ok(Map.of("data", link));
    }

    @GetMapping("/api/links/{code}/stats")
    public ResponseEntity<?> getStats(@PathVariable String code) {
        LinkStats stats = shortUrlService.getStats(code);
        if (stats == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Stats not found for this short code."));
        }
        return ResponseEntity.ok(Map.of("data", stats));
    }

    @GetMapping("/{code}")
    public RedirectView redirect(@PathVariable String code,
                                @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                @RequestHeader(value = "Referer", required = false) String referrer,
                                HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        ShortUrl link = shortUrlService.recordVisit(code, userAgent, referrer, ipAddress);
        if (link == null) {
            return new RedirectView("/404");
        }

        RedirectView redirectView = new RedirectView(link.getTargetUrl());
        redirectView.setStatusCode(HttpStatus.FOUND);
        return redirectView;
    }
}
