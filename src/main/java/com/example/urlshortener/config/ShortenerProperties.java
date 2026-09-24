package com.example.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Runtime config sourced from application.yml / env vars.
 * Env vars: SHORTENER_DB_PATH, SHORTENER_BASE_URL, SHORTENER_CODE_LENGTH,
 *           SHORTENER_CREATE_RATE_PER_MINUTE
 */
@ConfigurationProperties(prefix = "shortener")
public class ShortenerProperties {

    private String dbPath = "shortener.db";
    private String baseUrl = "http://localhost:8080";
    private int codeLength = 7;
    private int createRatePerMinute = 60;
    private final int maxUrlLength = 2048;
    private final int maxCodeAttempts = 5;

    public String getDbPath() { return dbPath; }
    public void setDbPath(String dbPath) { this.dbPath = dbPath; }

    public String getBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public int getCodeLength() { return codeLength; }
    public void setCodeLength(int codeLength) { this.codeLength = codeLength; }

    public int getCreateRatePerMinute() { return createRatePerMinute; }
    public void setCreateRatePerMinute(int v) { this.createRatePerMinute = v; }

    public int getMaxUrlLength() { return maxUrlLength; }
    public int getMaxCodeAttempts() { return maxCodeAttempts; }
}
