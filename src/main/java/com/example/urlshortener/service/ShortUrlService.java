package com.example.urlshortener.service;

import com.example.urlshortener.config.ShortenerProperties;
import com.example.urlshortener.exception.ApiException;
import com.example.urlshortener.exception.DuplicateCodeException;
import com.example.urlshortener.model.CreateLinkRequest;
import com.example.urlshortener.repository.Link;
import com.example.urlshortener.repository.LinkRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Merged URL-shortener service.
 *
 * Replaces the original in-memory ConcurrentHashMap implementation with
 * SQLite persistence (LinkRepository), SecureRandom code generation (CodeGenerator),
 * proper URL validation (UrlValidator), and token-bucket rate limiting (TokenBucketLimiter).
 *
 * API is backward-compatible with the original ShortUrlService callers.
 */
@Service
public class ShortUrlService {

    private final ShortenerProperties props;
    private final LinkRepository repository;
    private final CodeGenerator codeGenerator;
    private final UrlValidator urlValidator;
    private final TokenBucketLimiter limiter;
    private final ClickRecorder clickRecorder;
    private final String ownHost;

    public ShortUrlService(ShortenerProperties props,
                           LinkRepository repository,
                           CodeGenerator codeGenerator,
                           UrlValidator urlValidator,
                           TokenBucketLimiter limiter,
                           ClickRecorder clickRecorder,
                           String ownHost) {
        this.props = props;
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.urlValidator = urlValidator;
        this.limiter = limiter;
        this.clickRecorder = clickRecorder;
        this.ownHost = ownHost;
    }

    /**
     * Create a short link.
     * - Custom alias: 409 if already taken.
     * - Auto-generated code: idempotent (same URL returns same existing code with 200).
     * Returns the Link; callers decide the HTTP status (201 new, 200 dedup).
     */
    public LinkResult createShortUrl(CreateLinkRequest request) {
        String url = urlValidator.normalizeTargetUrl(request.url(), props.getMaxUrlLength(), ownHost);
        double now = System.currentTimeMillis() / 1000.0;

        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            String err = codeGenerator.aliasError(request.customAlias());
            if (err != null) throw new ApiException(err);
            Link link = repository.insertLink(request.customAlias(), url, true, now);
            return new LinkResult(link, true);
        }

        Optional<Link> existing = repository.findGeneratedLink(url);
        if (existing.isPresent()) {
            return new LinkResult(existing.get(), false);
        }

        for (int i = 0; i < props.getMaxCodeAttempts(); i++) {
            try {
                Link link = repository.insertLink(codeGenerator.generateCode(props.getCodeLength()), url, false, now);
                return new LinkResult(link, true);
            } catch (DuplicateCodeException ignored) {
                // code collision — retry
            }
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "could not generate a unique code");
    }

    public Optional<Link> getByCode(String code) {
        return repository.getLink(code);
    }

    /**
     * Record a redirect visit (rate-limited) and return the target URL.
     * Throws 429 if rate limit exceeded, 404 if code unknown.
     */
    public Link recordVisit(String code, String clientKey, String referrer) {
        if (!limiter.allow(clientKey)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "rate limit exceeded");
        }
        Link link = repository.getLink(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "short link not found"));
        clickRecorder.record(code, referrer);
        return link;
    }

    public boolean deleteLink(String code) {
        return repository.deleteLink(code);
    }

    public java.util.Map<String, Object> getStats(String code) {
        // Verify the code exists first so we return 404, not empty stats
        repository.getLink(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "short link not found"));
        return repository.clickStats(code, 5);
    }

    /** Wraps a Link with a flag indicating whether it was newly created (201) or found (200). */
    public record LinkResult(Link link, boolean isNew) {}
}
