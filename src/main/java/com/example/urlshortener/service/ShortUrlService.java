package com.example.urlshortener.service;

import com.example.urlshortener.exception.ApiException;
import com.example.urlshortener.model.CreateLinkRequest;
import com.example.urlshortener.model.LinkStats;
import com.example.urlshortener.model.ShortUrl;
import com.example.urlshortener.model.VisitEvent;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class ShortUrlService {
    private final Map<String, ShortUrl> shortUrls = new ConcurrentHashMap<>();
    private final Map<String, ShortUrl> redirectCache = new ConcurrentHashMap<>();
    private final Map<String, String> idempotencyIndex = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> requestHistory = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1000);
    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final long RATE_LIMIT_WINDOW_MS = 60_000L;

    public List<ShortUrl> listLinks() {
        return shortUrls.values().stream()
                .sorted(Comparator.comparing(ShortUrl::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public ShortUrl createShortUrl(CreateLinkRequest request) {
        if (request == null || request.url() == null || request.url().isBlank()) {
            throw new ApiException("A valid URL is required.");
        }

        String normalizedUrl = normalizeUrl(request.url());

        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            String existingCode = idempotencyIndex.get(request.idempotencyKey());
            if (existingCode != null && shortUrls.containsKey(existingCode)) {
                return shortUrls.get(existingCode);
            }
        }

        String alias = normalizeAlias(request.alias());
        String code = alias != null ? alias : generateCode();

        if (shortUrls.containsKey(code)) {
            throw new ApiException("This custom alias is already in use. Please choose another one.");
        }

        Instant now = Instant.now();
        Instant expiresAt = request.expiresAt() == null || request.expiresAt().isBlank()
                ? null
                : Instant.parse(request.expiresAt());

        String baseUrl = System.getenv().getOrDefault("APP_BASE_URL", "http://localhost:8080");
        ShortUrl shortUrl = new ShortUrl(
                UUID.randomUUID().toString(),
                code,
                normalizedUrl,
                baseUrl + "/" + code,
                now,
                now,
                expiresAt,
                true,
                new ArrayList<>(),
                0
        );

        shortUrls.put(code, shortUrl);
        redirectCache.put(code, shortUrl);

        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            idempotencyIndex.put(request.idempotencyKey(), code);
        }

        return shortUrl;
    }

    public ShortUrl getByCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }

        ShortUrl cached = redirectCache.get(code);
        if (cached != null && (cached.getExpiresAt() == null || !cached.getExpiresAt().isBefore(Instant.now()))) {
            return cached;
        }
        redirectCache.remove(code);

        ShortUrl link = shortUrls.get(code);
        if (link == null) {
            return null;
        }

        if (link.getExpiresAt() != null && link.getExpiresAt().isBefore(Instant.now())) {
            shortUrls.remove(code);
            idempotencyIndex.values().removeIf(value -> value.equals(code));
            return null;
        }

        redirectCache.put(code, link);
        return link;
    }

    public ShortUrl recordVisit(String code, String userAgent, String referrer, String ipAddress) {
        enforceRateLimit(ipAddress);
        ShortUrl link = getByCode(code);
        if (link == null) {
            return null;
        }

        VisitEvent event = new VisitEvent(Instant.now(), userAgent, referrer, ipAddress);
        link.recordVisit(event);
        link.incrementClicks();
        redirectCache.put(code, link);
        return link;
    }

    public void reset() {
        shortUrls.clear();
        redirectCache.clear();
        idempotencyIndex.clear();
        requestHistory.clear();
        sequence.set(1000);
    }

    public LinkStats getStats(String code) {
        ShortUrl link = getByCode(code);
        if (link == null) {
            return null;
        }

        Map<String, Long> referrers = link.getAnalytics().stream()
                .filter(event -> event.getReferrer() != null && !event.getReferrer().isBlank())
                .collect(Collectors.groupingBy(
                        event -> {
                            String ref = event.getReferrer();
                            if (ref.equalsIgnoreCase("direct") || ref.equals("-")) {
                                return "direct";
                            }
                            try {
                                return new URI(ref).getHost();
                            } catch (URISyntaxException e) {
                                return "direct";
                            }
                        },
                        Collectors.counting()
                ));

        List<VisitEvent> recent = link.getAnalytics().stream()
                .sorted(Comparator.comparing(VisitEvent::getVisitedAt).reversed())
                .limit(10)
                .collect(Collectors.toList());

        int uniqueVisitors = (int) link.getAnalytics().stream()
                .map(VisitEvent::getIpAddress)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new LinkStats(
                link.getCode(),
                link.getTargetUrl(),
                link.getClicks(),
                uniqueVisitors,
                link.getCreatedAt(),
                link.getExpiresAt(),
                link.isActive(),
                referrers,
                recent
        );
    }

    private void enforceRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return;
        }

        String key = "ip:" + ipAddress;
        Deque<Long> timestamps = requestHistory.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        while (!timestamps.isEmpty() && now - timestamps.peekFirst() > RATE_LIMIT_WINDOW_MS) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
            throw new ApiException("Rate limit exceeded. Please retry later.");
        }

        timestamps.offerLast(now);
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            int value = sequence.getAndIncrement() % 62;
            builder.append(charForValue(value));
        }
        return builder.toString();
    }

    private char charForValue(int value) {
        if (value < 10) {
            return (char) ('0' + value);
        }
        if (value < 36) {
            return (char) ('a' + value - 10);
        }
        return (char) ('A' + value - 36);
    }

    private String normalizeUrl(String rawUrl) {
        try {
            URI uri = new URI(rawUrl);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new ApiException("Only http and https protocols are supported.");
            }
            return uri.toString();
        } catch (Exception e) {
            throw new ApiException("A valid URL is required.");
        }
    }

    private String normalizeAlias(String alias) {
        if (alias == null || alias.isBlank()) {
            return null;
        }

        String normalized = alias.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9-]", "-");
        normalized = normalized.replaceAll("-+", "-");
        normalized = normalized.replaceAll("^-|-$", "");

        if (normalized.isBlank()) {
            return null;
        }

        return normalized.substring(0, Math.min(normalized.length(), 32));
    }
}
