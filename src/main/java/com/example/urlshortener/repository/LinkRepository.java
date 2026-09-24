package com.example.urlshortener.repository;

import com.example.urlshortener.config.ShortenerProperties;
import com.example.urlshortener.exception.DuplicateCodeException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SQLite persistence for links and click events.
 *
 * Uses a Hikari pool capped at maximumPoolSize=1 — two load-bearing reasons:
 *   1. SQLite only supports one writer at a time; a single connection mirrors the
 *      Python original's single-connection-plus-lock design.
 *   2. A ":memory:" DB only persists for one connection's lifetime — a fresh-connection-
 *      per-call DataSource would silently see an empty DB on the second query.
 */
@Repository
public class LinkRepository {

    private static final String SCHEMA = """
            CREATE TABLE IF NOT EXISTS links (
                code        TEXT PRIMARY KEY,
                url         TEXT NOT NULL,
                created_at  REAL NOT NULL,
                is_custom   INTEGER NOT NULL DEFAULT 0
            );
            CREATE INDEX IF NOT EXISTS idx_links_url ON links(url);
            CREATE TABLE IF NOT EXISTS clicks (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                code           TEXT NOT NULL,
                ts             REAL NOT NULL,
                referrer_host  TEXT
            );
            CREATE INDEX IF NOT EXISTS idx_clicks_code_ts ON clicks(code, ts);
            """;

    private static final String LINK_COLUMNS = "code, url, created_at, is_custom";

    private final JdbcTemplate jdbc;
    private final String dbPath;

    public LinkRepository(ShortenerProperties props) {
        this.dbPath = props.getDbPath();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(1);
        this.jdbc = new JdbcTemplate(new HikariDataSource(config));
    }

    @PostConstruct
    public void init() {
        if (!":memory:".equals(dbPath)) {
            jdbc.execute("PRAGMA journal_mode=WAL");
        }
        for (String statement : SCHEMA.split(";")) {
            if (!statement.isBlank()) {
                jdbc.execute(statement.trim());
            }
        }
    }

    public Link insertLink(String code, String url, boolean isCustom, double createdAt) {
        try {
            jdbc.update(
                    "INSERT INTO links (code, url, created_at, is_custom) VALUES (?, ?, ?, ?)",
                    code, url, createdAt, isCustom ? 1 : 0);
        } catch (org.springframework.dao.DataAccessException exc) {
            String message = String.valueOf(exc.getMostSpecificCause().getMessage());
            if (message.contains("UNIQUE constraint failed") || message.contains("CONSTRAINT_PRIMARYKEY")) {
                throw new DuplicateCodeException(code);
            }
            throw exc;
        }
        return new Link(code, url, createdAt, isCustom);
    }

    public Optional<Link> getLink(String code) {
        List<Link> rows = jdbc.query(
                "SELECT " + LINK_COLUMNS + " FROM links WHERE code = ?",
                this::mapLink, code);
        return rows.stream().findFirst();
    }

    /** Returns an existing auto-generated link for {@code url} (idempotent create). */
    public Optional<Link> findGeneratedLink(String url) {
        List<Link> rows = jdbc.query(
                "SELECT " + LINK_COLUMNS + " FROM links WHERE url = ? AND is_custom = 0 "
                        + "ORDER BY created_at LIMIT 1",
                this::mapLink, url);
        return rows.stream().findFirst();
    }

    public boolean deleteLink(String code) {
        jdbc.update("DELETE FROM clicks WHERE code = ?", code);
        int rows = jdbc.update("DELETE FROM links WHERE code = ?", code);
        return rows > 0;
    }

    public void recordClick(String code, String referrer, double ts) {
        jdbc.update(
                "INSERT INTO clicks (code, ts, referrer_host) VALUES (?, ?, ?)",
                code, ts, referrerHost(referrer));
    }

    public Map<String, Object> clickStats(String code, int topN) {
        int total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM clicks WHERE code = ?", Integer.class, code);

        List<Map<String, Object>> byDay = jdbc.query(
                "SELECT date(ts, 'unixepoch') AS day, COUNT(*) AS clicks FROM clicks "
                        + "WHERE code = ? GROUP BY day ORDER BY day",
                (rs, rowNum) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("day", rs.getString("day"));
                    m.put("clicks", rs.getInt("clicks"));
                    return m;
                }, code);

        List<Map<String, Object>> referrers = jdbc.query(
                "SELECT COALESCE(referrer_host, '(direct)') AS referrer, COUNT(*) AS clicks "
                        + "FROM clicks WHERE code = ? GROUP BY referrer ORDER BY clicks DESC, referrer LIMIT ?",
                (rs, rowNum) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("referrer", rs.getString("referrer"));
                    m.put("clicks", rs.getInt("clicks"));
                    return m;
                }, code, topN);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", code);
        result.put("total_clicks", total);
        result.put("clicks_by_day", byDay);
        result.put("top_referrers", referrers);
        return result;
    }

    private Link mapLink(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Link(
                rs.getString("code"),
                rs.getString("url"),
                rs.getDouble("created_at"),
                rs.getInt("is_custom") != 0);
    }

    private String referrerHost(String referrer) {
        if (referrer == null || referrer.isBlank()) return null;
        try {
            String host = new URI(referrer).getHost();
            if (host == null) return null;
            return host.length() > 255 ? host.substring(0, 255) : host;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
