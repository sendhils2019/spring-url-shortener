# Architecture

## Overview

This service combines two responsibilities in one Spring Boot application:

1. **URL Shortener** — stateful link storage, redirects, and click analytics
2. **Agentic Workflow Engine** — multi-stage AI workflow orchestration with governance controls

Both share the same HTTP server, Spring context, and SQLite database.

---

## Request Flow — URL Shortening

```
Client
  │
  ▼
ShortUrlController          (HTTP layer — validates input, maps status codes)
  │
  ▼
ShortUrlService             (business logic — dedup, alias validation, rate check)
  │         │
  │         ▼
  │     UrlValidator        (http/https only, no credentials, no self-referencing)
  │         │
  │         ▼
  │     CodeGenerator       (SecureRandom base-62, 7-char keyspace ≈ 3.5 trillion)
  │         │
  ▼         ▼
LinkRepository              (SQLite via JdbcTemplate, HikariCP pool=1)
  │
  ▼
SQLite DB  (shortener.db or :memory: in tests)
```

### Redirect path (hot path)

```
GET /{code}
  │
  ▼
ShortUrlController.redirect()
  │  → LinkRepository.getLink(code)       — sync DB read
  │  → 307 Location: <original-url>       — returned immediately
  │
  └─ ClickRecorder.record()               — @Async, off the hot path
       └─ LinkRepository.recordClick()    — writes referrer + timestamp
```

---

## Request Flow — Agentic Workflows

```
Client
  │
  ▼
AgenticWorkflowController   (REST: create, advance, replan, fallback, safe-stop)
  │
  ▼
AgenticWorkflowService      (in-memory workflow state: stages, decision log, audit trail)
  │
  ├── createWorkflow()      → builds stages based on scope (greenfield / brownfield)
  ├── replanWorkflow()      → injects risk-review gate if requirements are ambiguous
  ├── fallbackWorkflow()    → records rollback target and fallback reason
  ├── safeStopWorkflow()    → sets status=safe_stopped, appends to audit trail
  └── getMetrics()          → aggregates successRate, retryCount, rollbackCount, mttrMinutes
```

---

## Layer Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        HTTP (port 8081)                     │
├──────────────────────────┬──────────────────────────────────┤
│   ShortUrlController     │   AgenticWorkflowController      │
├──────────────────────────┼──────────────────────────────────┤
│   ShortUrlService        │   AgenticWorkflowService         │
│   UrlValidator           │                                  │
│   CodeGenerator          │                                  │
│   TokenBucketLimiter     │                                  │
│   ClickRecorder (@Async) │                                  │
├──────────────────────────┴──────────────────────────────────┤
│                     LinkRepository                          │
│                  (JdbcTemplate + SQLite)                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Design Decisions

### SQLite with pool size = 1
SQLite allows only one writer at a time. HikariCP is capped at `maximum-pool-size: 1` to prevent write contention. This works well for moderate traffic. To scale horizontally, swap the datasource for PostgreSQL — only `LinkRepository` and `application.yml` need to change.

### 307 Temporary Redirect (not 301/302)
`307` tells browsers not to cache the redirect. This ensures every visit is counted and that changing a link's destination takes effect immediately.

### Async click recording
`ClickRecorder` is annotated `@Async` and runs on Spring's task executor. The redirect response is returned to the client before the click is written to the database — keeping redirect latency minimal.

### Idempotent URL creation
If the same URL is submitted twice without a custom alias, the second request returns `200 OK` with the existing link instead of `201 Created`. This prevents duplicate entries and is safe to retry.

### Token bucket rate limiting
`TokenBucketLimiter` is a per-IP in-memory limiter. The bucket refills at a configurable rate (`shortener.create-rate-per-minute`). Tests set this to 1000 so they never hit 429.

### In-memory DB for tests
Tests use `@TestPropertySource(properties = {"shortener.db-path=:memory:"})`. Each test class gets its own fresh Spring context with a brand-new in-memory SQLite database — no shared state, no `reset()` method needed.

### Reserved aliases
The following codes are blocked from use as custom aliases to prevent route shadowing:
`api`, `docs`, `redoc`, `openapi.json`, `healthz`, `health`, `readyz`

---

## Configuration

Managed by `ShortenerProperties` (`@ConfigurationProperties(prefix="shortener")`):

| Property | Default | Notes |
|---|---|---|
| `db-path` | `shortener.db` | `:memory:` for tests |
| `base-url` | `http://localhost:8081` | Prepended to generated short URLs |
| `code-length` | `7` | Characters in generated codes |
| `create-rate-per-minute` | `60` | Token bucket refill rate |

The datasource URL is derived from `db-path` in `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:sqlite:${SHORTENER_DB_PATH:shortener.db}
    driver-class-name: org.sqlite.JDBC
    hikari:
      maximum-pool-size: 1
```

---

## Database Schema

```sql
CREATE TABLE IF NOT EXISTS links (
    code       TEXT PRIMARY KEY,
    url        TEXT NOT NULL,
    created_at REAL NOT NULL,
    is_custom  INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS clicks (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    code       TEXT NOT NULL,
    referrer   TEXT,
    clicked_at REAL NOT NULL,
    FOREIGN KEY (code) REFERENCES links(code) ON DELETE CASCADE
);
```

---

## Observability

| Endpoint | Description |
|---|---|
| `GET /health` | `{"status":"ok"}` — liveness check |
| `GET /docs` | Swagger UI (springdoc-openapi) |
| `GET /api/links/{code}/stats` | Click count + breakdown by referrer |
| `GET /api/agent/metrics` | Workflow reliability signals |

---

## Future Migration: SQLite → PostgreSQL

1. Replace `sqlite-jdbc` with `postgresql` driver in `pom.xml`
2. Update `application.yml` datasource URL and driver class
3. Remove `maximum-pool-size: 1` (Postgres supports concurrent writes)
4. No Java code changes required — `LinkRepository` uses standard JDBC SQL
