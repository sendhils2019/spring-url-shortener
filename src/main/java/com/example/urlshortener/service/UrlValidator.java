package com.example.urlshortener.service;

import com.example.urlshortener.exception.ApiException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/** URL validation: rejects schemes and shapes commonly abused by shorteners. */
@Component
public class UrlValidator {

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    public String normalizeTargetUrl(String raw, int maxLength, String ownHost) {
        String url = raw == null ? "" : raw.strip();
        if (url.isEmpty()) {
            throw new ApiException("url must not be empty");
        }
        if (url.length() > maxLength) {
            throw new ApiException("url must be at most " + maxLength + " characters");
        }
        for (int i = 0; i < url.length(); i++) {
            char ch = url.charAt(i);
            if (Character.isWhitespace(ch) || ch < 0x20 || ch == 0x7F) {
                throw new ApiException("url must not contain whitespace or control characters");
            }
        }

        URI parts;
        try {
            parts = new URI(url);
        } catch (URISyntaxException exc) {
            throw new ApiException("url is malformed");
        }

        String scheme = parts.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new ApiException("only http and https URLs are allowed");
        }

        String host = parts.getHost();
        if (host == null || host.isEmpty()) {
            throw new ApiException("url must include a host");
        }

        String userInfo = parts.getUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            throw new ApiException("url must not contain credentials");
        }

        if (ownHost != null) {
            String a = stripTrailingDot(host.toLowerCase());
            String b = stripTrailingDot(ownHost.toLowerCase());
            if (a.equals(b)) {
                throw new ApiException("url must not point back to this shortener");
            }
        }

        int port = parts.getPort();
        String authority = host.toLowerCase() + (port == -1 ? "" : ":" + port);
        String path = parts.getRawPath() == null ? "" : parts.getRawPath();
        String query = parts.getRawQuery() == null ? "" : "?" + parts.getRawQuery();
        String fragment = parts.getRawFragment() == null ? "" : "#" + parts.getRawFragment();

        return scheme.toLowerCase() + "://" + authority + path + query + fragment;
    }

    private String stripTrailingDot(String s) {
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }
}
