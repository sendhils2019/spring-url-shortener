package com.example.urlshortener.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Set;
import java.util.regex.Pattern;

/** Random base62 code generation and custom-alias validation. */
@Component
public class CodeGenerator {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{3,32}$");

    private static final Set<String> RESERVED_ALIASES =
            Set.of("api", "docs", "redoc", "openapi.json", "healthz", "health", "readyz");

    private final SecureRandom random = new SecureRandom();

    /** Random, non-enumerable base62 code (62^7 ≈ 3.5e12 keyspace at length 7). */
    public String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** Returns a human-readable error if {@code alias} is not allowed, else null. */
    public String aliasError(String alias) {
        if (!ALIAS_PATTERN.matcher(alias).matches()) {
            return "custom_alias must be 3-32 characters of letters, digits, '-' or '_'";
        }
        if (RESERVED_ALIASES.contains(alias.toLowerCase())) {
            return "custom_alias '" + alias + "' is reserved";
        }
        return null;
    }
}
